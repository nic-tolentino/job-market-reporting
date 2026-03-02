package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.ApifyJobDto
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class MappedSyncData(val companies: List<CompanyRecord>, val jobs: List<JobRecord>)

/** A wrapper for raw job data that includes its original ingestion timestamp. */
data class RawJob(val dto: ApifyJobDto, val ingestedAt: Instant)

/** Key used to identify logical roles for deduplication. */
data class DedupKey(val company: String, val title: String)

/**
 * Responsible for transforming raw job DTOs from external sources (Apify/LinkedIn) into structured
 * and deduplicated [JobRecord] and [CompanyRecord] entities.
 */
@Service
class RawJobDataMapper(private val parser: RawJobDataParser) {

        private val log = LoggerFactory.getLogger(RawJobDataMapper::class.java)

        private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        private val PHONE_REGEX =
                Regex("(?:\\+?6[14][\\s-]?)?\\(?0?[\\d]{1,4}\\)?[\\s-]?[\\d]{3,4}[\\s-]?[\\d]{3,4}")

        /** Removes sensitive contact information from job descriptions. */
        fun sanitize(text: String?): String? {
                if (text == null) return null
                return text.replace(EMAIL_REGEX, "[REDACTED EMAIL]")
                        .replace(PHONE_REGEX, "[REDACTED PHONE]")
        }

        /**
         * Transforms a list of raw job DTOs into a structured and deduplicated collection of
         * companies and jobs.
         */
        fun map(syncedJobs: List<RawJob>): MappedSyncData {
                // Step 1: Filter
                val validJobs = syncedJobs.filter { !it.dto.id.isNullOrBlank() }

                // Step 2: Group by DedupKey
                val groups =
                        validJobs.groupBy { (dto, _) ->
                                DedupKey(
                                        company = tidyCompanyName(dto.companyName),
                                        title = dto.title?.lowercase()?.trim() ?: "unknown"
                                )
                        }

                // Step 3: Process Representative & Aggregate
                val companyRecords = mutableMapOf<String, CompanyRecord>()
                val jobRecords = mutableListOf<JobRecord>()

                groups.forEach { (key, group) ->
                        try {
                                val representative = group.first()
                                val ingestedAt = representative.ingestedAt

                                // 3a. Parse Job Details (Heavy parsing happens here)
                                val jobRecord = parseJobDetails(group, ingestedAt)
                                jobRecords.add(jobRecord)

                                // 3b. Parse Company Metadata
                                if (!companyRecords.containsKey(key.company)) {
                                        val companyRecord = parseCompanyMetadata(group, ingestedAt)
                                        companyRecords[key.company] = companyRecord
                                } else {
                                        val existing = companyRecords[key.company]!!
                                        val updatedTechs =
                                                (existing.technologies + jobRecord.technologies)
                                                        .distinct()
                                                        .sorted()
                                        val updatedLocations =
                                                (existing.hiringLocations + jobRecord.locations)
                                                        .distinct()
                                                        .sorted()
                                        companyRecords[key.company] =
                                                existing.copy(
                                                        technologies = updatedTechs,
                                                        hiringLocations = updatedLocations
                                                )
                                }
                        } catch (e: Exception) {
                                log.error(
                                        "Failed to map group for key: $key. Error: ${e.message}",
                                        e
                                )
                        }
                }

                log.info(
                        "Mapping complete: ${syncedJobs.size} raw -> ${jobRecords.size} jobs, ${companyRecords.size} companies"
                )
                return MappedSyncData(companyRecords.values.toList(), jobRecords)
        }

        private fun parseJobDetails(group: List<RawJob>, ingestedAt: Instant): JobRecord {
                val first = group.first().dto
                val title = first.title ?: "Unknown Title"
                val companyName = first.companyName ?: "Unknown Company"
                val companyId = tidyCompanyName(companyName)

                val jobIds = group.mapNotNull { it.dto.id }.distinct()
                val applyUrls = group.mapNotNull { it.dto.applyUrl }.distinct()
                val links = group.mapNotNull { it.dto.link }.distinct()

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
                val (city, stateRegion, country) = parser.parseLocation(first.location)

                return JobRecord(
                        jobIds = jobIds,
                        applyUrls = applyUrls,
                        links = links,
                        locations = locations,
                        companyId = companyId,
                        companyName = companyName,
                        source = "LinkedIn",
                        country =
                                if (country != "Unknown") country
                                else parser.determineCountry(first.location),
                        city = city,
                        stateRegion = stateRegion,
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
                        postedDate =
                                group.firstNotNullOfOrNull { parser.parseDate(it.dto.postedAt) },
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
                        ingestedAt = ingestedAt
                )
        }

        private fun parseCompanyMetadata(group: List<RawJob>, ingestedAt: Instant): CompanyRecord {
                val first = group.first().dto
                val companyName = first.companyName ?: "Unknown Company"
                val companyId = tidyCompanyName(companyName)
                val description = sanitize(first.companyDescription)
                val roleTechs =
                        parser.extractTechnologies(
                                group.firstNotNullOfOrNull { it.dto.descriptionText } ?: ""
                        )
                val roleLocations =
                        group
                                .map { raw ->
                                        val (city, state, _) =
                                                parser.parseLocation(raw.dto.location)
                                        if (state == "Unknown" || state == city) city
                                        else "$city, $state"
                                }
                                .distinct()
                                .sorted()

                return CompanyRecord(
                        companyId = companyId,
                        name = companyName,
                        logoUrl = first.companyLogo,
                        description = description,
                        website = first.companyWebsite,
                        employeesCount = first.companyEmployeesCount,
                        industries = first.industries,
                        technologies = roleTechs,
                        hiringLocations = roleLocations,
                        ingestedAt = ingestedAt
                )
        }

        private fun tidyCompanyName(name: String?): String {
                return (name ?: "Unknown Company")
                        .lowercase()
                        .replace(Regex("[^a-z0-9]+"), "-")
                        .trim('-')
                        .ifBlank { "unknown" }
        }
}
