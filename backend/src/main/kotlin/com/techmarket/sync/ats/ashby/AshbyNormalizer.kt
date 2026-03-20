package com.techmarket.sync.ats.ashby

import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.sync.ats.AtsNormalizer
import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.util.HtmlUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Normalizes raw Ashby Job Board API responses into [NormalizedJob] instances.
 *
 * Ashby v2 API returns (current):
 * ```json
 * {
 *   "apiVersion": "...",
 *   "organization": { "name": "..." },
 *   "jobs": [ { ... }, ... ]
 * }
 * ```
 * Legacy format used `jobPostings` as the root key — both are supported.
 *
 * Key fields per posting (v2 → v1 fallback):
 *  - `id`                — stable UUID
 *  - `title`             — job title
 *  - `location`          — primary location string (was `locationName`)
 *  - `secondaryLocations`— array of additional location strings (was `allLocations`)
 *  - `team`              — department / team (was `teamName`)
 *  - `employmentType`    — "FullTime" | "PartTime" | "Contract" | "Internship" | "Temporary"
 *  - `isRemote`          — boolean hint for work model
 *  - `workplaceType`     — "Remote" | "Hybrid" | "OnSite" (v2 addition)
 *  - `publishedAt`       — ISO 8601 datetime
 *  - `jobUrl`            — canonical job page URL
 *  - `applyUrl`          — direct apply URL
 *  - `descriptionHtml`   — full job description as HTML
 *  - `descriptionPlain`  — pre-stripped plain text (preferred over re-stripping HTML)
 *  - `compensation`      — optional salary block
 */
@Component
class AshbyNormalizer : AtsNormalizer {

    private val log = LoggerFactory.getLogger(AshbyNormalizer::class.java)

    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        // v2 API uses "jobs"; legacy API used "jobPostings"
        val postingsNode = rawData.get("jobs") ?: rawData.get("jobPostings")
        if (postingsNode == null || !postingsNode.isArray) {
            log.warn("Ashby: Response missing 'jobs' (or 'jobPostings') array.")
            return emptyList()
        }

        return postingsNode.mapNotNull { node ->
            try {
                mapJob(node)
            } catch (e: Exception) {
                log.error("Ashby: Failed to normalize job node: ${e.message}", e)
                null
            }
        }
    }

    private fun mapJob(job: JsonNode): NormalizedJob {
        val platformId =
            job.get("id")?.asText()
                ?: throw IllegalArgumentException("Ashby job missing required 'id' field")

        val title = job.get("title")?.asText()

        // Location:
        //   v2 format: "location" (primary string) + "secondaryLocations" (extras) → join all
        //   v1 format: "locationName" (primary) + "allLocations" (ALL locations incl. primary) → join allLocations
        val location: String? = when {
            job.has("secondaryLocations") -> {
                // v2: location is primary; secondaryLocations are extras
                val primary   = job.path("location").asText(null)
                val secondary = job.path("secondaryLocations")
                val parts = mutableListOf<String>()
                if (!primary.isNullOrBlank()) parts.add(primary)
                if (secondary.isArray) {
                    (0 until secondary.size()).mapNotNullTo(parts) { secondary.get(it).asText(null) }
                }
                parts.ifEmpty { null }?.joinToString(", ")
            }
            job.path("allLocations").isArray && job.path("allLocations").size() > 0 -> {
                // v1: allLocations contains ALL locations (already includes primary)
                val all = job.path("allLocations")
                (0 until all.size()).mapNotNull { all.get(it).asText(null) }.joinToString(", ")
            }
            else -> job.path("location").asText(null) ?: job.path("locationName").asText(null)
        }

        // Department: v2 uses "team"; v1 used "teamName"
        val department = job.path("team").asText(null)
            ?: job.path("teamName").asText(null)

        val employmentType = normalizeEmploymentType(job.path("employmentType").asText(null))

        // Work model: prefer workplaceType (v2), fall back to isRemote boolean
        val workModel = when (job.path("workplaceType").asText(null)) {
            "Remote" -> "Remote"
            "Hybrid" -> "Hybrid"
            "OnSite" -> "On-site"
            else -> when {
                job.path("isRemote").asBoolean(false) -> "Remote"
                else -> null
            }
        }

        val publishedAt = job.get("publishedAt")?.asText()
        val jobUrl   = job.get("jobUrl")?.asText()
        val applyUrl = job.get("applyUrl")?.asText()

        val descriptionHtml  = job.get("descriptionHtml")?.asText()
        // Prefer Ashby's pre-stripped plain text over re-stripping HTML
        val descriptionText  = job.get("descriptionPlain")?.asText()
            ?: HtmlUtils.stripHtml(descriptionHtml)

        // Salary: pull from compensation.summaryComponents[0]
        val compensation = job.path("compensation")
        val salaryComponent = compensation.path("summaryComponents").path(0)
        val salaryMin      = salaryComponent.path("minValue").asDouble(0.0).takeIf { it > 0 }?.toInt()
        val salaryMax      = salaryComponent.path("maxValue").asDouble(0.0).takeIf { it > 0 }?.toInt()
        val salaryCurrency = compensation.path("currency").asText(null)
            ?: salaryComponent.path("format").asText(null)

        return NormalizedJob(
            platformId       = platformId,
            source           = "Ashby",
            title            = title,
            companyName      = "", // filled by AtsJobDataSyncService after normalization
            location         = location,
            descriptionHtml  = descriptionHtml,
            descriptionText  = descriptionText,
            salaryMin        = salaryMin,
            salaryMax        = salaryMax,
            salaryCurrency   = salaryCurrency,
            employmentType   = employmentType,
            seniorityLevel   = null,
            workModel        = workModel,
            department       = department,
            postedAt         = publishedAt,
            firstPublishedAt = publishedAt,
            applyUrl         = applyUrl ?: jobUrl,
            platformUrl      = jobUrl,
            rawPayload       = job.toString()
        )
    }

    private fun normalizeEmploymentType(raw: String?): String? = when (raw) {
        "FullTime"   -> "Full-time"
        "PartTime"   -> "Part-time"
        "Contract"   -> "Contract"
        "Internship" -> "Internship"
        "Temporary"  -> "Temporary"
        else         -> raw
    }
}
