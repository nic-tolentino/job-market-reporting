package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.ApifyJobDto
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
                    // Languages
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
                    "ruby",
                    "php",
                    "swift",
                    "objective-c",
                    "scala",
                    "dart",
                    "elixir",
                    "clojure",
                    "haskell",
                    "lua",
                    "perl",
                    "r",
                    "shell",
                    "bash",

                    // Frontend Frameworks & Libs
                    "react",
                    "angular",
                    "vue",
                    "nextjs",
                    "next.js",
                    "svelte",
                    "ember",
                    "backbone",
                    "html",
                    "css",
                    "sass",
                    "less",
                    "tailwind",
                    "bootstrap",
                    "material-ui",
                    "redux",
                    "graphql",

                    // Backend Frameworks
                    "spring",
                    "spring boot",
                    "django",
                    "flask",
                    "fastapi",
                    "node",
                    "nodejs",
                    "node.js",
                    "express",
                    "nest",
                    "nestjs",
                    "ruby on rails",
                    "laravel",
                    "asp.net",
                    "dotnet",
                    ".net",

                    // Mobile
                    "android",
                    "ios",
                    "flutter",
                    "react native",
                    "xamarin",
                    "ionic",
                    "kotlin multiplatform",

                    // Cloud & Infrastructure
                    "aws",
                    "gcp",
                    "azure",
                    "docker",
                    "kubernetes",
                    "k8s",
                    "terraform",
                    "ansible",
                    "chef",
                    "puppet",
                    "jenkins",
                    "github actions",
                    "gitlab ci",
                    "circleci",
                    "travis ci",
                    "linux",
                    "ubuntu",
                    "serverless",
                    "lambda",
                    "cloudformation",

                    // Databases & Storage
                    "sql",
                    "postgresql",
                    "postgres",
                    "mysql",
                    "mongodb",
                    "mongo",
                    "redis",
                    "elasticsearch",
                    "cassandra",
                    "dynamodb",
                    "mariadb",
                    "oracle",
                    "sql server",
                    "sqlite",
                    "couchbase",
                    "neo4j",

                    // Data & Analytics
                    "bigquery",
                    "snowflake",
                    "redshift",
                    "hadoop",
                    "spark",
                    "kafka",
                    "rabbitmq",
                    "activemq",
                    "airflow",
                    "dbt",
                    "databricks",
                    "pandas",
                    "numpy",
                    "scikit-learn",
                    "tensorflow",
                    "pytorch"
            )

    fun mapSyncData(apifyJobs: List<ApifyJobDto>): MappedSyncData {
        val jobs = mutableListOf<JobRecord>()
        // Track raw company metadata as we encounter companies
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
        // Collect all technologies per company across all jobs
        val companyTechSets = mutableMapOf<String, MutableSet<String>>()

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

                if (!companyMetas.containsKey(companyId)) {
                    companyMetas[companyId] =
                            CompanyMeta(
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

                val techs = extractTechnologies(dto.descriptionText ?: "")
                companyTechSets.getOrPut(companyId) { mutableSetOf() }.addAll(techs)

                // TODO: When adding new sources, ensure the `jobId` uniquely identifies the job
                // across all platforms.
                // We will likely need to construct a composite key (e.g., "${source}-${dto.id}")
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
                                technologies = techs,
                                salaryMin = parseSalary(dto.salaryInfo?.firstOrNull()),
                                salaryMax = parseSalary(dto.salaryInfo?.lastOrNull()),
                                postedDate = parseDate(dto.postedAt),
                                benefits = dto.benefits,
                                employmentType = dto.employmentType,
                                workModel = extractWorkModel(dto.location, dto.title),
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

        // Build company records with aggregated technologies from all their jobs
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
                            technologies = companyTechSets[meta.companyId]?.sorted() ?: emptyList(),
                            ingestedAt = meta.ingestedAt
                    )
                }

        return MappedSyncData(companies, jobs)
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

    private fun extractWorkModel(location: String?, title: String?): String {
        val locLower = location?.lowercase() ?: ""
        val titleLower = title?.lowercase() ?: ""
        val combined = "$locLower $titleLower"
        return when {
            combined.contains("remote") -> "Remote"
            combined.contains("hybrid") -> "Hybrid"
            else -> "On-site"
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
