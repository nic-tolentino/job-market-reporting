package com.jobmarket.app.sync

import com.jobmarket.app.persistence.model.CompanyRecord
import com.jobmarket.app.persistence.model.JobRecord
import com.jobmarket.app.sync.model.ApifyJobDto
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

data class MappedSyncData(val companies: List<CompanyRecord>, val jobs: List<JobRecord>)

@Service
class JobDataMapper {

    private val techKeywords =
            setOf(
                    "kotlin",
                    "java",
                    "python",
                    "go",
                    "golang",
                    "rust",
                    "c++",
                    "c#",
                    "javascript",
                    "typescript",
                    "react",
                    "angular",
                    "vue",
                    "nextjs",
                    "node",
                    "aws",
                    "gcp",
                    "azure",
                    "docker",
                    "kubernetes",
                    "kafka",
                    "rabbitmq",
                    "sql",
                    "postgresql",
                    "mysql",
                    "mongodb",
                    "bigquery",
                    "snowflake"
            )

    fun mapSyncData(apifyJobs: List<ApifyJobDto>): MappedSyncData {
        val jobs = mutableListOf<JobRecord>()
        val companiesMap = mutableMapOf<String, CompanyRecord>()

        apifyJobs.filter { !it.id.isNullOrBlank() }.forEach { dto ->
            try {
                val companyName = dto.companyName ?: "Unknown Company"
                val companyId =
                        companyName
                                .lowercase()
                                .replace(Regex("[^a-z0-9]+"), "-")
                                .trim('-')
                                .ifBlank { "unknown" }

                val ingestedAt = Instant.now()

                if (!companiesMap.containsKey(companyId)) {
                    companiesMap[companyId] =
                            CompanyRecord(
                                    companyId = companyId,
                                    name = companyName,
                                    logoUrl = dto.companyLogo,
                                    description = dto.companyDescription,
                                    website = dto.companyWebsite,
                                    employeesCount = dto.companyEmployeesCount,
                                    industries = dto.industries,
                                    ingestedAt = ingestedAt
                            )
                }

                // TODO: When adding new sources, ensure the `jobId` uniquely identifies the job
                // across all platforms.
                // We will likely need to construct a composite key (e.g., "\${source}-\${dto.id}")
                // for storage.
                jobs.add(
                        JobRecord(
                                jobId = dto.id!!,
                                companyId = companyId,
                                companyName = companyName,
                                source = "LinkedIn", // Hardcoded per requirements
                                country = determineCountry(dto.location),
                                title = dto.title ?: "Unknown Title",
                                location = dto.location ?: "Unknown Location",
                                seniorityLevel =
                                        extractSeniority(dto.title ?: "", dto.seniorityLevel),
                                technologies = extractTechnologies(dto.descriptionText ?: ""),
                                salaryMin = parseSalary(dto.salaryInfo?.firstOrNull()),
                                salaryMax = parseSalary(dto.salaryInfo?.lastOrNull()),
                                postedDate = parseDate(dto.postedAt),
                                benefits = dto.benefits,
                                employmentType = dto.employmentType,
                                jobFunction = dto.jobFunction,
                                applyUrl = dto.applyUrl,
                                rawLocation = dto.location,
                                rawSeniorityLevel = dto.seniorityLevel,
                                ingestedAt = ingestedAt
                        )
                )
            } catch (e: Exception) {
                LoggerFactory.getLogger(JobDataMapper::class.java)
                        .error("Failed to map job record ID: ${dto.id}. Error: ${e.message}", e)
            }
        }

        return MappedSyncData(companiesMap.values.toList(), jobs)
    }

    private fun determineCountry(location: String?): String {
        if (location == null) return "Unknown"
        val locUpper = location.uppercase()
        return when {
            locUpper.contains("AUSTRALIA") ||
                    locUpper.contains(" Sydney") ||
                    locUpper.contains(" Melbourne") -> "AU"
            locUpper.contains("NEW ZEALAND") ||
                    locUpper.contains(" Auckland") ||
                    locUpper.contains(" Wellington") -> "NZ"
            locUpper.contains("SPAIN") ||
                    locUpper.contains(" Madrid") ||
                    locUpper.contains(" Barcelona") -> "ES"
            else -> "Unknown"
        }
    }

    private fun extractSeniority(title: String, existingLevel: String?): String {
        val t = title.lowercase()
        return when {
            t.contains("senior") || t.contains(" sr.") || t.contains(" sr ") -> "Senior"
            t.contains("lead") || t.contains("principal") || t.contains("staff") -> "Lead/Principal"
            t.contains("junior") || t.contains(" jr.") || t.contains(" jr ") -> "Junior"
            !existingLevel.isNullOrBlank() -> existingLevel // Fallback to provided structured data
            else -> "Mid-Level"
        }
    }

    private fun extractTechnologies(description: String): List<String> {
        val descLower = description.lowercase()
        return techKeywords.filter { keyword ->
            descLower.contains(Regex("\\b${Regex.escape(keyword)}\\b"))
        }
    }

    private fun parseSalary(salaryStr: String?): Int? {
        if (salaryStr == null) return null
        return try {
            val digits = salaryStr.replace(Regex("[^0-9]"), "")
            if (digits.isNotBlank()) digits.toInt() else null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseDate(dateStr: String?): LocalDate? {
        if (dateStr == null) return null
        return try {
            LocalDate.parse(dateStr)
        } catch (e: DateTimeParseException) {
            null
        }
    }
}
