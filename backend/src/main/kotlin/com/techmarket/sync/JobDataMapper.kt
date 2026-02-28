package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.ApifyJobDto
import java.time.Instant
import java.time.LocalDate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class MappedSyncData(val companies: List<CompanyRecord>, val jobs: List<JobRecord>)

@Service
class JobDataMapper(private val parser: JobDataParser) {

        private val log = LoggerFactory.getLogger(JobDataMapper::class.java)

        /** Intermediate representation for a single raw job posting before deduplication. */
        private data class RawJobEntry(
                val jobId: String,
                val companyId: String,
                val companyName: String,
                val title: String,
                val normalizedTitle: String,
                val seniorityLevel: String,
                val location: String,
                val applyUrl: String?,
                val description: String?,
                val technologies: List<String>,
                val salaryMin: Int?,
                val salaryMax: Int?,
                val postedDate: LocalDate?,
                val benefits: List<String>?,
                val employmentType: String?,
                val workModel: String,
                val jobFunction: String?,
                val rawLocation: String?,
                val rawSeniorityLevel: String?,
                val ingestedAt: Instant,
                // Raw company metadata
                val companyLogoUrl: String?,
                val companyDescription: String?,
                val companyWebsite: String?,
                val companyEmployeesCount: Int?,
                val companyIndustries: String?,
                val link: String?
        )

        private val EMAIL_REGEX = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        private val PHONE_REGEX =
                Regex("(?:\\+?6[14][\\s-]?)?\\(?0?[\\d]{1,4}\\)?[\\s-]?[\\d]{3,4}[\\s-]?[\\d]{3,4}")

        private fun sanitize(text: String?): String? {
                if (text == null) return null
                return text.replace(EMAIL_REGEX, "[REDACTED EMAIL]")
                        .replace(PHONE_REGEX, "[REDACTED PHONE]")
        }

        fun mapSyncData(apifyJobs: List<ApifyJobDto>): MappedSyncData {
                val ingestedAt = Instant.now()

                // --- PASS 1: Parse every raw job posting into an intermediate record ---
                val rawEntries = mutableListOf<RawJobEntry>()
                apifyJobs.filter { !it.id.isNullOrBlank() }.forEach { dto ->
                        try {
                                val companyName = dto.companyName ?: "Unknown Company"
                                val companyId =
                                        companyName
                                                .lowercase()
                                                .replace(Regex("[^a-z0-9]+"), "-")
                                                .trim('-')
                                                .ifBlank { "unknown" }

                                val title = dto.title ?: "Unknown Title"
                                val seniorityLevel =
                                        parser.extractSeniority(title, dto.seniorityLevel)
                                val techs = parser.extractTechnologies(dto.descriptionText ?: "")
                                val location = dto.location ?: "Unknown Location"

                                rawEntries.add(
                                        RawJobEntry(
                                                jobId = dto.id!!,
                                                companyId = companyId,
                                                companyName = companyName,
                                                title = title,
                                                normalizedTitle = title.lowercase().trim(),
                                                seniorityLevel = seniorityLevel,
                                                location = location,
                                                applyUrl = dto.applyUrl,
                                                description = dto.descriptionHtml?.ifBlank { null }
                                                                ?: dto.descriptionText,
                                                technologies = techs,
                                                salaryMin =
                                                        parser.parseSalary(
                                                                dto.salaryInfo?.firstOrNull()
                                                        ),
                                                salaryMax =
                                                        parser.parseSalary(
                                                                dto.salaryInfo?.lastOrNull()
                                                        ),
                                                postedDate = parser.parseDate(dto.postedAt),
                                                benefits = dto.benefits,
                                                employmentType = dto.employmentType,
                                                workModel =
                                                        parser.extractWorkModel(location, title),
                                                jobFunction = dto.jobFunction,
                                                rawLocation = location,
                                                rawSeniorityLevel = dto.seniorityLevel,
                                                ingestedAt = ingestedAt,
                                                companyLogoUrl = dto.companyLogo,
                                                companyDescription =
                                                        sanitize(dto.companyDescription),
                                                companyWebsite = dto.companyWebsite,
                                                companyEmployeesCount = dto.companyEmployeesCount,
                                                companyIndustries = dto.industries,
                                                link = dto.link
                                        )
                                )
                        } catch (e: Exception) {
                                log.error(
                                        "Failed to map job record ID: ${dto.id}. Error: ${e.message}",
                                        e
                                )
                        }
                }

                // --- PASS 2: Deduplicate by (companyId, normalizedTitle, seniorityLevel) ---
                // Each group = one deduplicated job with multiple locations
                data class DedupKey(
                        val companyId: String,
                        val normalizedTitle: String,
                        val seniorityLevel: String
                )

                val groups = LinkedHashMap<DedupKey, MutableList<RawJobEntry>>()
                rawEntries.forEach { entry ->
                        val key =
                                DedupKey(
                                        entry.companyId,
                                        entry.normalizedTitle,
                                        entry.seniorityLevel
                                )
                        groups.getOrPut(key) { mutableListOf() }.add(entry)
                }

                // --- PASS 3: Build JobRecords and CompanyRecords ---
                // Per-company accumulators
                data class CompanyMeta(
                        val companyId: String,
                        val name: String,
                        val logoUrl: String?,
                        val description: String?,
                        val website: String?,
                        val employeesCount: Int?,
                        val industries: String?,
                        val ingestedAt: Instant
                )
                val companyMetas = mutableMapOf<String, CompanyMeta>()
                val companyTechSets = mutableMapOf<String, MutableSet<String>>()
                val companyLocationSets = mutableMapOf<String, MutableSet<String>>()

                val jobs =
                        groups.values.map { group ->
                                val first = group.first()

                                // Record company metadata from the first occurrence we see
                                if (!companyMetas.containsKey(first.companyId)) {
                                        companyMetas[first.companyId] =
                                                CompanyMeta(
                                                        companyId = first.companyId,
                                                        name = first.companyName,
                                                        logoUrl = first.companyLogoUrl,
                                                        description = first.companyDescription,
                                                        website = first.companyWebsite,
                                                        employeesCount =
                                                                first.companyEmployeesCount,
                                                        industries = first.companyIndustries,
                                                        ingestedAt = ingestedAt
                                                )
                                }

                                // Union technologies across all duplicates
                                val allTechs = group.flatMap { it.technologies }.toSet().sorted()
                                companyTechSets
                                        .getOrPut(first.companyId) { mutableSetOf() }
                                        .addAll(allTechs)

                                val locations =
                                        group.map { rawEntry ->
                                                val (c, s, _) =
                                                        parser.parseLocation(rawEntry.rawLocation)
                                                if (s == "Unknown" || s == c) c else "$c, $s"
                                        }
                                companyLocationSets
                                        .getOrPut(first.companyId) { mutableSetOf() }
                                        .addAll(locations)

                                // Description is from the first available entry
                                val description =
                                        sanitize(group.firstNotNullOfOrNull { it.description })
                                val jobIds = group.map { it.jobId }
                                val applyUrls = group.map { it.applyUrl ?: "" }
                                val links = group.map { it.link ?: "" }

                                val (city, stateRegion, country) =
                                        parser.parseLocation(first.rawLocation)

                                JobRecord(
                                        jobIds = jobIds,
                                        applyUrls = applyUrls,
                                        links = links,
                                        locations = locations,
                                        companyId = first.companyId,
                                        companyName = first.companyName,
                                        source = "LinkedIn",
                                        country =
                                                if (country != "Unknown") country
                                                else parser.determineCountry(first.rawLocation),
                                        city = city,
                                        stateRegion = stateRegion,
                                        title = first.title,
                                        seniorityLevel = first.seniorityLevel,
                                        technologies = allTechs,
                                        // First non-null wins for scalar fields
                                        salaryMin = group.firstNotNullOfOrNull { it.salaryMin },
                                        salaryMax = group.firstNotNullOfOrNull { it.salaryMax },
                                        postedDate = group.firstNotNullOfOrNull { it.postedDate },
                                        benefits = group.firstNotNullOfOrNull { it.benefits }
                                                        ?: emptyList(),
                                        employmentType =
                                                group.firstNotNullOfOrNull { it.employmentType },
                                        workModel = group.firstNotNullOfOrNull { it.workModel }
                                                        ?: "On-site",
                                        jobFunction = group.firstNotNullOfOrNull { it.jobFunction },
                                        description = description,
                                        ingestedAt = ingestedAt
                                )
                        }

                log.info(
                        "Deduplication: ${rawEntries.size} raw postings → ${jobs.size} unique roles"
                )

                val companies =
                        companyMetas.values.map { meta ->
                                CompanyRecord(
                                        companyId = meta.companyId,
                                        name = meta.name,
                                        logoUrl = meta.logoUrl,
                                        description = meta.description,
                                        website = meta.website,
                                        employeesCount = meta.employeesCount,
                                        industries = meta.industries,
                                        technologies = companyTechSets[meta.companyId]?.sorted()
                                                        ?: emptyList(),
                                        hiringLocations =
                                                companyLocationSets[meta.companyId]?.sorted()
                                                        ?: emptyList(),
                                        ingestedAt = meta.ingestedAt
                                )
                        }

                return MappedSyncData(companies, jobs)
        }
}
