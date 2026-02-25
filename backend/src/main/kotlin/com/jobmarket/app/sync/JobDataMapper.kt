package com.jobmarket.app.sync

import com.jobmarket.app.persistence.model.BigQueryJobRecord
import com.jobmarket.app.sync.model.ApifyJobDto
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

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

    fun mapToBigQueryRecords(apifyJobs: List<ApifyJobDto>): List<BigQueryJobRecord> {
        val records = mutableListOf<BigQueryJobRecord>()

        apifyJobs.filter { !it.id.isNullOrBlank() }.forEach { dto ->
            try {
                records.add(
                        BigQueryJobRecord(
                                job_id = dto.id!!,
                                source = "LinkedIn", // Hardcoded per requirements
                                country = determineCountry(dto.location),
                                title = dto.title ?: "Unknown Title",
                                company = dto.companyName ?: "Unknown Company",
                                location = dto.location ?: "Unknown Location",
                                seniority_level =
                                        extractSeniority(dto.title ?: "", dto.seniorityLevel),
                                technologies = extractTechnologies(dto.descriptionText ?: ""),
                                salary_min = parseSalary(dto.salaryInfo?.firstOrNull()),
                                salary_max = parseSalary(dto.salaryInfo?.lastOrNull()),
                                posted_date = parseDate(dto.postedAt),
                                raw_description = dto.descriptionText,
                                companyLogo = dto.companyLogo,
                                benefits = dto.benefits,
                                applicantsCount = dto.applicantsCount,
                                applyUrl = dto.applyUrl,
                                jobPosterName = dto.jobPosterName,
                                employmentType = dto.employmentType,
                                jobFunction = dto.jobFunction,
                                industries = dto.industries,
                                companyDescription = dto.companyDescription,
                                companyWebsite = dto.companyWebsite,
                                companyEmployeesCount = dto.companyEmployeesCount,
                                raw_location = dto.location,
                                raw_seniority_level = dto.seniorityLevel,
                                ingested_at = java.time.Instant.now()
                        )
                )
            } catch (e: Exception) {
                // Log the exact DTO ID that failed to parse so we can debug schema changes later
                LoggerFactory.getLogger(JobDataMapper::class.java)
                        .error("Failed to map job record ID: ${dto.id}. Error: ${e.message}", e)
            }
        }

        return records
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
            else -> "Unknown" // We can refine this later or map from a specific input source
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
            // Simple bound matching to avoid partial word hits (e.g. matching "go" inside "good")
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
