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

    private val log = LoggerFactory.getLogger(JobDataMapper::class.java)

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
            val companyIndustries: String?
    )

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
                val seniorityLevel = extractSeniority(title, dto.seniorityLevel)
                val techs = extractTechnologies(dto.descriptionText ?: "")
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
                                technologies = techs,
                                salaryMin = parseSalary(dto.salaryInfo?.firstOrNull()),
                                salaryMax = parseSalary(dto.salaryInfo?.lastOrNull()),
                                postedDate = parseDate(dto.postedAt),
                                benefits = dto.benefits,
                                employmentType = dto.employmentType,
                                workModel = extractWorkModel(location, title),
                                jobFunction = dto.jobFunction,
                                rawLocation = location,
                                rawSeniorityLevel = dto.seniorityLevel,
                                ingestedAt = ingestedAt,
                                companyLogoUrl = dto.companyLogo,
                                companyDescription = dto.companyDescription,
                                companyWebsite = dto.companyWebsite,
                                companyEmployeesCount = dto.companyEmployeesCount,
                                companyIndustries = dto.industries
                        )
                )
            } catch (e: Exception) {
                log.error("Failed to map job record ID: ${dto.id}. Error: ${e.message}", e)
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
            val key = DedupKey(entry.companyId, entry.normalizedTitle, entry.seniorityLevel)
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
                                        employeesCount = first.companyEmployeesCount,
                                        industries = first.companyIndustries,
                                        ingestedAt = ingestedAt
                                )
                    }

                    // Union technologies across all duplicates
                    val allTechs = group.flatMap { it.technologies }.toSet().sorted()
                    companyTechSets.getOrPut(first.companyId) { mutableSetOf() }.addAll(allTechs)

                    // Collect all locations (no deduplication per spec — same city can appear
                    // twice)
                    val allLocations = group.map { it.location }
                    companyLocationSets
                            .getOrPut(first.companyId) { mutableSetOf() }
                            .addAll(allLocations)

                    JobRecord(
                            jobIds = group.map { it.jobId },
                            applyUrls = group.map { it.applyUrl },
                            locations = allLocations,
                            companyId = first.companyId,
                            companyName = first.companyName,
                            source = "LinkedIn",
                            country = determineCountry(first.rawLocation),
                            title = first.title,
                            seniorityLevel = first.seniorityLevel,
                            technologies = allTechs,
                            // First non-null wins for scalar fields
                            salaryMin = group.firstNotNullOfOrNull { it.salaryMin },
                            salaryMax = group.firstNotNullOfOrNull { it.salaryMax },
                            postedDate = group.firstNotNullOfOrNull { it.postedDate },
                            benefits = group.firstNotNullOfOrNull { it.benefits },
                            employmentType = group.firstNotNullOfOrNull { it.employmentType },
                            workModel = group.firstNotNullOfOrNull { it.workModel } ?: "On-site",
                            jobFunction = group.firstNotNullOfOrNull { it.jobFunction },
                            rawLocation = first.rawLocation,
                            rawSeniorityLevel = first.rawSeniorityLevel,
                            ingestedAt = ingestedAt
                    )
                }

        log.info("Deduplication: ${rawEntries.size} raw postings → ${jobs.size} unique roles")

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
                            hiringLocations = companyLocationSets[meta.companyId]?.sorted()
                                            ?: emptyList(),
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
            !existingLevel.isNullOrBlank() -> existingLevel
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
