package com.techmarket.sync.ats.teamtailor

import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.sync.ats.AtsNormalizer
import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.util.HtmlUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Normalizes raw TeamTailor JSON feed responses into [NormalizedJob] instances.
 *
 * TeamTailor returns a top-level JSON array from `{slug}.teamtailor.com/feed/jobs.json`:
 * ```json
 * [
 *   {
 *     "id": 123456,
 *     "title": "Software Engineer",
 *     "apply_url": "https://company.teamtailor.com/jobs/123456-software-engineer",
 *     "human_status": "open",
 *     "created_at": "2024-01-15T09:00:00.000+11:00",
 *     "body": "<p>Job description HTML</p>",
 *     "tags": ["engineering"],
 *     "remote_status": "none",
 *     "locations": [{ "city": "Sydney", "country": "Australia" }],
 *     "categories": [{ "name": "Engineering" }],
 *     "department": { "name": "Product" }
 *   }
 * ]
 * ```
 */
@Component
class TeamTailorNormalizer : AtsNormalizer {

    private val log = LoggerFactory.getLogger(TeamTailorNormalizer::class.java)

    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        if (!rawData.isArray) {
            log.warn("TeamTailor: Response is not a JSON array.")
            return emptyList()
        }

        return rawData.mapNotNull { node ->
            try {
                mapJob(node)
            } catch (e: Exception) {
                log.error("TeamTailor: Failed to normalize job node: ${e.message}", e)
                null
            }
        }
    }

    private fun mapJob(job: JsonNode): NormalizedJob {
        val platformId = job.get("id")?.asText()
            ?: throw IllegalArgumentException("TeamTailor job missing required 'id' field")

        val title = job.get("title")?.asText()

        // Location: join all location entries
        val locations = job.path("locations")
        val location = if (locations.isArray && locations.size() > 0) {
            (0 until locations.size()).mapNotNull { i ->
                val loc = locations.get(i)
                val city    = loc.path("city").asText(null)
                val country = loc.path("country").asText(null)
                listOfNotNull(city, country).joinToString(", ").ifBlank { null }
            }.joinToString("; ")
        } else null

        val workModel = when (job.path("remote_status").asText(null)?.lowercase()) {
            "fully_remote", "remote" -> "Remote"
            "hybrid"                -> "Hybrid"
            else                    -> null
        }

        // Department: try "department.name", then first category name
        val department = job.path("department").path("name").asText(null)
            ?: job.path("categories").path(0).path("name").asText(null)

        val descriptionHtml = job.path("body").asText(null)
        val createdAt       = job.get("created_at")?.asText()
        val applyUrl        = job.get("apply_url")?.asText()

        return NormalizedJob(
            platformId       = platformId,
            source           = "TeamTailor",
            title            = title,
            companyName      = "",
            location         = location,
            descriptionHtml  = descriptionHtml,
            descriptionText  = HtmlUtils.stripHtml(descriptionHtml),
            salaryMin        = null,
            salaryMax        = null,
            salaryCurrency   = null,
            employmentType   = null, // not available in feed
            seniorityLevel   = null,
            workModel        = workModel,
            department       = department,
            postedAt         = createdAt,
            firstPublishedAt = createdAt,
            applyUrl         = applyUrl,
            platformUrl      = applyUrl,
            rawPayload       = job.toString()
        )
    }
}
