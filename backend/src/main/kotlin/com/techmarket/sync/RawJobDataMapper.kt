package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.ApifyJobDto
import com.techmarket.util.IdGenerator
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
class RawJobDataMapper(private val parser: RawJobDataParser) {

        private val log = LoggerFactory.getLogger(RawJobDataMapper::class.java)

        private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        private val PHONE_REGEX =
                Regex("(?:\\+?6[14][\\s-]?)?\\(?0?[\\d]{1,4}\\)?[\\s-]?[\\d]{3,4}[\\s-]?[\\d]{3,4}")

        /** Entry point for the mapping pipeline. */
        fun map(syncedJobs: List<RawJob>): MappedSyncData {
                val validJobs = filterValidJobs(syncedJobs)
                val roleGroups = groupByLogicalRole(validJobs)

                val allOpenings =
                        roleGroups.values.flatMap { jobsInRole -> groupByOpening(jobsInRole) }

                val mappedData = assembleMappedData(allOpenings)

                log.info(
                        "Mapping pipeline complete: ${syncedJobs.size} raw -> ${mappedData.jobs.size} jobs, ${mappedData.companies.size} companies"
                )
                return mappedData
        }

        /** Filters out raw records that are missing essential identification fields. */
        internal fun filterValidJobs(syncedJobs: List<RawJob>): List<RawJob> {
                return syncedJobs.filter { !it.dto.id.isNullOrBlank() }
        }

        /**
         * Groups raw postings by their logical role identity. Identity is defined as the
         * combination of (Company, Country, Title).
         */
        internal fun groupByLogicalRole(
                jobs: List<RawJob>
        ): Map<Triple<String, String, String>, List<RawJob>> {
                return jobs.groupBy { (dto, _) ->
                        val companyId = IdGenerator.buildCompanyId(dto.companyName)
                        val country = parser.determineCountry(dto.location).lowercase()
                        val titleSlug = IdGenerator.slugify(dto.title ?: "unknown")
                        Triple(companyId, country, titleSlug)
                }
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
        internal fun assembleMappedData(openingGroups: List<List<RawJob>>): MappedSyncData {
                val companyRecords = mutableMapOf<String, CompanyRecord>()
                val jobRecords = mutableListOf<JobRecord>()

                openingGroups.forEach { group ->
                        try {
                                // The last record in a lifecycle group represents the most recent
                                // state
                                val latestSnapshot = group.last()
                                val lastSeenAt = latestSnapshot.lastSeenAt

                                // 1. Map individual job record
                                val jobRecord = parseJobDetails(group, lastSeenAt)
                                jobRecords.add(jobRecord)

                                // 2. Map or update company record
                                val companySlug = IdGenerator.buildCompanyId(jobRecord.companyName)
                                if (!companyRecords.containsKey(companySlug)) {
                                        companyRecords[companySlug] =
                                                parseCompanyMetadata(group, lastSeenAt)
                                } else {
                                        val existing = companyRecords[companySlug]!!
                                        companyRecords[companySlug] =
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
        private fun parseJobDetails(group: List<RawJob>, lastSeenAt: Instant): JobRecord {
                val first = group.first().dto
                val title = first.title ?: "Unknown Title"
                val companyName = first.companyName ?: "Unknown Company"
                val companyId = IdGenerator.buildCompanyId(companyName)

                val platformJobIds = group.mapNotNull { it.dto.id }.distinct()
                val applyUrls = group.mapNotNull { it.dto.applyUrl }.distinct()
                val platformLinks = group.mapNotNull { it.dto.link }.distinct()

                val locations =
                        group
                                .map { raw ->
                                        val (city, state, _) =
                                                parser.parseLocation(raw.dto.location)
                                        if (state == "Unknown" || state == city) city
                                        else "$city, $state"
                                }
                                .distinct()
                                .sorted()

                val description =
                        sanitize(
                                group.firstNotNullOfOrNull {
                                        it.dto.descriptionHtml?.ifBlank { null }
                                                ?: it.dto.descriptionText
                                }
                        )
                val technologies =
                        parser.extractTechnologies(
                                group.firstNotNullOfOrNull { it.dto.descriptionText } ?: ""
                        )

                val (_, _, countryCode) = parser.parseLocation(first.location)
                val country =
                        if (countryCode != "Unknown") countryCode
                        else parser.determineCountry(first.location)

                // Find earliest date in the lifecycle for ID stability
                val allPostedDates = group.mapNotNull { parser.parseDate(it.dto.postedAt) }
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
                                group.firstNotNullOfOrNull {
                                        parser.parseSalary(it.dto.salaryInfo?.firstOrNull())
                                },
                        salaryMax =
                                group.firstNotNullOfOrNull {
                                        parser.parseSalary(it.dto.salaryInfo?.lastOrNull())
                                },
                        postedDate = earliestPostedDate,
                        benefits =
                                group
                                        .flatMap { it.dto.benefits ?: emptyList() }
                                        .distinct()
                                        .sorted(),
                        employmentType = group.firstNotNullOfOrNull { it.dto.employmentType },
                        workModel =
                                group.firstNotNullOfOrNull {
                                        parser.extractWorkModel(it.dto.location, it.dto.title)
                                }
                                        ?: "On-site",
                        jobFunction = group.firstNotNullOfOrNull { it.dto.jobFunction },
                        description = description,
                        lastSeenAt = lastSeenAt
                )
        }

        private fun parseCompanyMetadata(group: List<RawJob>, lastSeenAt: Instant): CompanyRecord {
                val first = group.first().dto
                val companyName = first.companyName ?: "Unknown Company"
                val companyId = IdGenerator.buildCompanyId(companyName)

                return CompanyRecord(
                        companyId = companyId,
                        name = companyName,
                        alternateNames = listOf(companyName),
                        logoUrl = first.companyLogo,
                        description = sanitize(first.companyDescription),
                        website = first.companyWebsite,
                        employeesCount = first.companyEmployeesCount,
                        industries = first.industries,
                        technologies =
                                parser.extractTechnologies(
                                        group.firstNotNullOfOrNull { it.dto.descriptionText } ?: ""
                                ),
                        hiringLocations =
                                group
                                        .map { raw ->
                                                val (city, state, _) =
                                                        parser.parseLocation(raw.dto.location)
                                                if (state == "Unknown" || state == city) city
                                                else "$city, $state"
                                        }
                                        .distinct()
                                        .sorted(),
                        lastUpdatedAt = lastSeenAt
                )
        }

        fun sanitize(text: String?): String? {
                if (text == null) return null
                return text.replace(EMAIL_REGEX, "[REDACTED EMAIL]")
                        .replace(PHONE_REGEX, "[REDACTED PHONE]")
        }
}
