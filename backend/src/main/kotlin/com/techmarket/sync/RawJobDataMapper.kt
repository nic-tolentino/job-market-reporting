package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.VerificationLevel
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.ApifyJobDto
import com.techmarket.util.IdGenerator
import com.techmarket.util.PiiSanitizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/** Result of the mapping process containing structured [CompanyRecord]s and [JobRecord]s. */
data class MappedSyncData(val companies: List<CompanyRecord>, val jobs: List<JobRecord>)

/**
 * A wrapper for raw job data that includes its original ingestion timestamp. This ingestion
 * timestamp is treated as the 'lastSeenAt' time for the job snapshot.
 */
data class RawJob(val dto: ApifyJobDto, val lastSeenAt: Instant)

/**
 * Responsible for transforming raw job DTOs from external sources (Apify, LinkedIn, etc.) into
 * structured, deduplicated entities for the Silver Layer.
 *
 * The mapping process follows these stages:
 * 1. **Filter**: Remove records with missing platform IDs.
 * 2. **Group by Role**: Aggregate postings by Company, Country, and Title.
 * 3. **Group by Opening**: Segment role postings into "Hiring Events" using lifecycle logic.
 * 4. **Assemble**: Transform groups into final Records.
 */
@Service
class RawJobDataMapper(
        private val parser: RawJobDataParser,
        private val classifier: TechRoleClassifier
) {

        private val log = LoggerFactory.getLogger(RawJobDataMapper::class.java)

        /** Entry point for the mapping pipeline. */
        fun map(
                syncedJobs: List<RawJob>,
                manifestCompanies: Map<String, CompanyRecord> = emptyMap(),
                targetCountry: String? = null
        ): MappedSyncData {
                val validJobs = filterValidJobs(syncedJobs)
                val roleGroups = groupByLogicalRole(validJobs, manifestCompanies, targetCountry)

                val allOpenings =
                        roleGroups.values.flatMap { jobsInRole -> groupByOpening(jobsInRole) }

                val mappedData = assembleMappedData(allOpenings, manifestCompanies, targetCountry)

                log.info(
                        "Mapping pipeline complete: ${syncedJobs.size} raw -> ${mappedData.jobs.size} jobs, ${mappedData.companies.size} companies"
                )
                return mappedData
        }

        /** Filters out raw records that are missing essential identification fields. */
        internal fun filterValidJobs(syncedJobs: List<RawJob>): List<RawJob> {
                return syncedJobs.filter {
                        !it.dto.id.isNullOrBlank() && classifier.isTechRole(it.dto)
                }
        }

        /**
         * Groups raw postings by their logical role identity. Identity is defined as the
         * combination of (Company, Country, Title).
         */
        internal fun groupByLogicalRole(
                jobs: List<RawJob>,
                manifestCompanies: Map<String, CompanyRecord> = emptyMap(),
                targetCountry: String? = null
        ): Map<Triple<String, String, String>, List<RawJob>> {
                return jobs.groupBy { (dto, _) ->
                        val companyId = findCompanyId(dto.companyName, manifestCompanies)
                        val country = targetCountry?.lowercase() ?: parser.determineCountry(dto.location).lowercase()
                        val titleSlug = IdGenerator.slugify(dto.title ?: "unknown")
                        Triple(companyId, country, titleSlug)
                }
        }

        internal fun findCompanyId(scrapedName: String?, manifestCompanies: Map<String, CompanyRecord>): String {
                if (scrapedName.isNullOrBlank()) return "unknown-company"

                // 1. Exact ID match (unlikely for scraped names)
                val slugifiedName = IdGenerator.buildCompanyId(scrapedName)
                if (manifestCompanies.containsKey(slugifiedName)) return manifestCompanies[slugifiedName]!!.companyId

                // 2. Multi-alias search
                manifestCompanies.values.forEach { company ->
                        if (company.name.equals(scrapedName, ignoreCase = true) ||
                                company.alternateNames.any { it.equals(scrapedName, ignoreCase = true) }) {
                                return company.companyId
                        }
                }
            
            // 3. Fallback to Ghost creation
            return "ghost-$slugifiedName"
        }

        /**
         * Clusters postings into "Hiring Events" (Openings) based on job lifecycle.
         *
         * Logic: A job belongs to an existing opening if its postedDate falls between the opening's
         * start and its most recent "last seen" date (plus a small 8-day buffer to account for
         * platform refreshes).
         */
        internal fun groupByOpening(jobs: List<RawJob>): List<List<RawJob>> {
                if (jobs.isEmpty()) return emptyList()

                // Sort by postedDate to process lifecycle chronologically
                val sortedJobs =
                        jobs.sortedBy { parser.parseDate(it.dto.postedAt) ?: LocalDate.MIN }

                val openings = mutableListOf<List<RawJob>>()
                var currentOpening = mutableListOf<RawJob>()

                // Initial window bounds
                var openingStartDate: LocalDate? = null
                var lastSeenBoundary: LocalDate? = null

                sortedJobs.forEach { job ->
                        val jobDate = parser.parseDate(job.dto.postedAt)
                        val jobIngestedDate =
                                job.lastSeenAt.atZone(ZoneId.systemDefault()).toLocalDate()

                        if (currentOpening.isEmpty()) {
                                currentOpening.add(job)
                                openingStartDate = jobDate
                                lastSeenBoundary = jobIngestedDate
                        } else {
                                // Rule: Is the new postedDate within 14 days of the previous
                                // last-seen?
                                // Or is it between the original postedDate and the lastSeen date?
                                val isSameOpening =
                                        when {
                                                openingStartDate == null || jobDate == null -> true
                                                lastSeenBoundary == null -> true
                                                // 1. Between posted and last seen?
                                                !jobDate.isBefore(openingStartDate) &&
                                                        !jobDate.isAfter(lastSeenBoundary) -> true
                                                // 2. Or within 14 days of the last seen date?
                                                // (Handles "refreshed" postings)
                                                ChronoUnit.DAYS.between(
                                                        lastSeenBoundary,
                                                        jobDate
                                                ) <= 14 -> true
                                                else -> false
                                        }

                                if (isSameOpening) {
                                        currentOpening.add(job)
                                        // Extend the boundary if this snapshot is newer
                                        if (jobIngestedDate.isAfter(lastSeenBoundary)) {
                                                lastSeenBoundary = jobIngestedDate
                                        }
                                } else {
                                        openings.add(currentOpening)
                                        currentOpening = mutableListOf(job)
                                        openingStartDate = jobDate
                                        lastSeenBoundary = jobIngestedDate
                                }
                        }
                }
                if (currentOpening.isNotEmpty()) openings.add(currentOpening)

                return openings
        }

        /** Transforms clustered openings into final persistence-ready Records. */
        internal fun assembleMappedData(
                openingGroups: List<List<RawJob>>,
                manifestCompanies: Map<String, CompanyRecord> = emptyMap(),
                targetCountry: String? = null
        ): MappedSyncData {
                val companyRecords = mutableMapOf<String, CompanyRecord>()
                val jobRecords = mutableListOf<JobRecord>()

                openingGroups.forEach { group ->
                        try {
                                // The last record in a lifecycle group represents the most recent
                                // state
                                val latestSnapshot = group.last()
                                val lastSeenAt = latestSnapshot.lastSeenAt

                                // 1. Map individual job record
                                val jobRecord = parseJobDetails(group, lastSeenAt, manifestCompanies, targetCountry)
                                // 2. Map or update company record
                                val companyId = jobRecord.companyId
                                
                                // Blocked/Spam Filter
                                if (manifestCompanies[companyId]?.verificationLevel == VerificationLevel.BLOCKED) {
                                    log.info("Skipping job ${jobRecord.title} from blocked company: $companyId")
                                    return@forEach
                                }

                                if (!companyId.startsWith("ghost-") && manifestCompanies.containsKey(companyId)) {
                                    // It's a manifest company, we don't need to re-create it in this mapper's output,
                                    // as they are synced separately from companies.json.
                                    jobRecords.add(jobRecord)
                                    return@forEach
                                }
                                
                                jobRecords.add(jobRecord)

                                if (!companyRecords.containsKey(companyId)) {
                                        companyRecords[companyId] =
                                                parseCompanyMetadata(group, lastSeenAt, manifestCompanies)
                                } else {
                                        val existing = companyRecords[companyId]!!
                                        companyRecords[companyId] =
                                                existing.copy(
                                                         technologies =
                                                                 (existing.technologies +
                                                                                 jobRecord
                                                                                         .technologies)
                                                                         .distinct()
                                                                         .sorted(),
                                                         hiringLocations =
                                                                 (existing.hiringLocations +
                                                                                 jobRecord.locations)
                                                                         .distinct()
                                                                         .sorted(),
                                                         lastUpdatedAt = lastSeenAt
                                                 )
                                 }
                        } catch (e: Exception) {
                                log.error("Failed to assemble record: ${e.message}", e)
                        }
                }

                return MappedSyncData(companyRecords.values.toList(), jobRecords)
        }

        /** Parses raw group data into a single [JobRecord]. */
        private fun parseJobDetails(
                lifecycle: List<RawJob>,
                lastSeenAt: Instant,
                manifestCompanies: Map<String, CompanyRecord>,
                targetCountry: String?
        ): JobRecord {
                val first = lifecycle.first().dto
                val title = first.title ?: "Unknown Title"
                val companyName = first.companyName ?: "Unknown Company"
                val companyId = findCompanyId(companyName, manifestCompanies)

                val platformJobIds = lifecycle.mapNotNull { it.dto.id }.distinct()
                val applyUrls = lifecycle.mapNotNull { it.dto.applyUrl }.distinct()
                val platformLinks = lifecycle.mapNotNull { it.dto.link }.distinct()

                val locations =
                        lifecycle
                                .map { raw ->
                                        val (city, state, _) =
                                                parser.parseLocation(raw.dto.location)
                                        if (state == "Unknown" || state == city) city
                                        else "$city, $state"
                                }
                                .distinct()
                                .sorted()

                val description =
                        PiiSanitizer.sanitize(
                                lifecycle.firstNotNullOfOrNull {
                                        it.dto.descriptionHtml?.ifBlank { null }
                                                ?: it.dto.descriptionText
                                }
                        )
                val technologies =
                        parser.extractTechnologies(
                                lifecycle.firstNotNullOfOrNull { it.dto.descriptionText } ?: ""
                        )

                val country = (targetCountry ?: run {
                        val (_, _, parsedCountry) = parser.parseLocation(first.location)
                        if (parsedCountry != "Unknown") parsedCountry
                        else parser.determineCountry(first.location)
                }).lowercase()

                // Find earliest date in the lifecycle for ID stability
                val allPostedDates = lifecycle.mapNotNull { parser.parseDate(it.dto.postedAt) }
                val earliestPostedDate = allPostedDates.minOrNull()

                val datePart =
                        earliestPostedDate?.toString()
                                ?: lastSeenAt
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                        .toString()
                val jobId = IdGenerator.buildJobId(companyId, country, title, datePart)

                return JobRecord(
                        jobId = jobId,
                        platformJobIds = platformJobIds,
                        applyUrls = applyUrls,
                        platformLinks = platformLinks,
                        locations = locations,
                        companyId = companyId,
                        companyName = companyName,
                        source = "LinkedIn",
                        country = country,
                        city = first.location ?: "Unknown",
                        stateRegion = "",
                        title = title,
                        seniorityLevel = parser.extractSeniority(title, first.seniorityLevel),
                        technologies = technologies,
                        salaryMin =
                                lifecycle.firstNotNullOfOrNull {
                                        parser.parseSalary(it.dto.salaryInfo?.firstOrNull())
                                },
                        salaryMax =
                                lifecycle.firstNotNullOfOrNull {
                                        parser.parseSalary(it.dto.salaryInfo?.lastOrNull())
                                },
                        postedDate = earliestPostedDate,
                        benefits =
                                lifecycle
                                        .flatMap { it.dto.benefits ?: emptyList() }
                                        .distinct()
                                        .sorted(),
                        employmentType = lifecycle.firstNotNullOfOrNull { it.dto.employmentType },
                        workModel =
                                lifecycle.firstNotNullOfOrNull {
                                        parser.extractWorkModel(it.dto.location, it.dto.title)
                                }
                                        ?: "On-site",
                        jobFunction = lifecycle.firstNotNullOfOrNull { it.dto.jobFunction },
                        description = description,
                        lastSeenAt = lastSeenAt
                )
        }

        private fun parseCompanyMetadata(
                lifecycle: List<RawJob>,
                lastSeenAt: Instant,
                manifestCompanies: Map<String, CompanyRecord>
        ): CompanyRecord {
                val first = lifecycle.first().dto
                val companyName = first.companyName ?: "Unknown Company"
                val companyId = findCompanyId(companyName, manifestCompanies)

                return CompanyRecord(
                        companyId = companyId,
                        name = companyName,
                        alternateNames = listOf(companyName),
                        logoUrl = first.companyLogo,
                        description = PiiSanitizer.sanitize(first.companyDescription),
                        website = first.companyWebsite,
                        employeesCount = first.companyEmployeesCount,
                        industries = first.industries,
                        technologies =
                                parser.extractTechnologies(
                                        lifecycle.firstNotNullOfOrNull { it.dto.descriptionText } ?: ""
                                ),
                        hiringLocations =
                                lifecycle
                                        .map { raw ->
                                                val (city, state, _) =
                                                        parser.parseLocation(raw.dto.location)
                                                if (state == "Unknown" || state == city) city
                                                else "$city, $state"
                                        }
                                        .distinct()
                                        .sorted(),
                        verificationLevel = VerificationLevel.GHOST,
                        lastUpdatedAt = lastSeenAt
                )
        }
}
