package com.techmarket.sync.ats.workable

import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.sync.ats.AtsNormalizer
import com.techmarket.sync.ats.model.NormalizedJob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Normalizes raw Workable API responses into [NormalizedJob] instances.
 *
 * Workable returns:
 * ```json
 * {
 *   "total": 13,
 *   "results": [
 *     {
 *       "id": "...",
 *       "shortcode": "ABC123",
 *       "title": "Software Engineer",
 *       "remote": true,
 *       "location": { "country": "Australia", "countryCode": "AU", "city": "Sydney", "region": "NSW" },
 *       "locations": [...],
 *       "state": "published",
 *       "type": "PF",
 *       "department": ["Engineering"],
 *       "workplace": "hybrid"
 *     }
 *   ]
 * }
 * ```
 *
 * The `shortcode` is used to build the apply URL: `https://apply.workable.com/{slug}/j/{shortcode}`
 * The job description is not available in the list response.
 */
@Component
class WorkableNormalizer : AtsNormalizer {

    private val log = LoggerFactory.getLogger(WorkableNormalizer::class.java)

    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        val resultsNode = rawData.get("results")
        if (resultsNode == null || !resultsNode.isArray) {
            log.warn("Workable: Response missing 'results' array.")
            return emptyList()
        }

        return resultsNode.mapNotNull { node ->
            try {
                mapJob(node)
            } catch (e: Exception) {
                log.error("Workable: Failed to normalize job node: ${e.message}", e)
                null
            }
        }
    }

    private fun mapJob(job: JsonNode): NormalizedJob {
        val platformId = job.get("id")?.asText()
            ?: throw IllegalArgumentException("Workable job missing required 'id' field")

        val shortcode = job.get("shortcode")?.asText()
        val title     = job.get("title")?.asText()

        // Location: primary + additional locations
        val primaryLocation   = buildLocationString(job.path("location"))
        val additionalLocations = job.path("locations")
        val allLocationParts = mutableListOf<String>()
        if (!primaryLocation.isNullOrBlank()) allLocationParts.add(primaryLocation)
        if (additionalLocations.isArray) {
            (0 until additionalLocations.size()).mapNotNullTo(allLocationParts) {
                buildLocationString(additionalLocations.get(it))
            }
        }
        val location = allLocationParts.distinct().joinToString("; ").ifBlank { null }

        val workModel = when (job.path("workplace").asText(null)?.lowercase()) {
            "remote"                             -> "Remote"
            "hybrid"                             -> "Hybrid"
            "onsite", "on-site", "on_site"       -> "On-site"
            else -> when {
                job.path("remote").asBoolean(false) -> "Remote"
                else                                -> null
            }
        }

        val employmentType = normalizeType(job.path("type").asText(null))

        // department is an array of strings
        val departmentNode = job.path("department")
        val department = when {
            departmentNode.isArray && departmentNode.size() > 0 ->
                (0 until departmentNode.size()).mapNotNull { departmentNode.get(it).asText(null) }.joinToString(", ")
            else -> null
        }

        return NormalizedJob(
            platformId       = platformId,
            source           = "Workable",
            title            = title,
            companyName      = "",
            location         = location,
            descriptionHtml  = null, // not available in list response
            descriptionText  = null,
            salaryMin        = null,
            salaryMax        = null,
            salaryCurrency   = null,
            employmentType   = employmentType,
            seniorityLevel   = null,
            workModel        = workModel,
            department       = department,
            postedAt         = null,
            firstPublishedAt = null,
            applyUrl         = shortcode?.let { "https://apply.workable.com/j/$it" },
            platformUrl      = shortcode?.let { "https://apply.workable.com/j/$it" },
            rawPayload       = job.toString()
        )
    }

    private fun buildLocationString(loc: JsonNode): String? {
        val parts = listOfNotNull(
            loc.path("city").asText(null),
            loc.path("region").asText(null),
            loc.path("country").asText(null)
        ).filter { it.isNotBlank() }
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }

    /** Maps Workable's internal type codes to readable strings. */
    private fun normalizeType(raw: String?): String? = when (raw?.uppercase()) {
        "PF"  -> "Full-time"
        "PP"  -> "Part-time"
        "PFL" -> "Freelance"
        "PIT" -> "Internship"
        "PCT" -> "Contract"
        "PT"  -> "Temporary"
        else  -> raw
    }
}
