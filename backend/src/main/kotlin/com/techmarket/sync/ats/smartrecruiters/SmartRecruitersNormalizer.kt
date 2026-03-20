package com.techmarket.sync.ats.smartrecruiters

import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.sync.ats.AtsNormalizer
import com.techmarket.sync.ats.model.NormalizedJob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Normalizes raw SmartRecruiters postings API responses into [NormalizedJob] instances.
 *
 * SmartRecruiters returns:
 * ```json
 * {
 *   "totalFound": 13,
 *   "content": [
 *     {
 *       "id": "...",
 *       "name": "Senior Engineer",
 *       "uuid": "...",
 *       "company": { "identifier": "carsales", "name": "Carsales" },
 *       "releasedDate": "2026-01-15T00:00:00.000+0000",
 *       "location": {
 *         "city": "Melbourne", "region": "VIC", "country": "AU",
 *         "remote": false, "hybrid": false, "fullLocation": "Melbourne, VIC, AU"
 *       },
 *       "department": { "id": "...", "label": "Engineering" },
 *       "typeOfEmployment": { "id": "permanent", "label": "Permanent" },
 *       "experienceLevel": { "id": "mid_senior_level", "label": "Mid-Senior level" },
 *       "customField": []
 *     }
 *   ]
 * }
 * ```
 *
 * Note: the list API does not include job descriptions — those require a separate per-job call
 * to `/v1/companies/{slug}/postings/{id}` which is not performed here.
 */
@Component
class SmartRecruitersNormalizer : AtsNormalizer {

    private val log = LoggerFactory.getLogger(SmartRecruitersNormalizer::class.java)

    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        val contentNode = rawData.get("content")
        if (contentNode == null || !contentNode.isArray) {
            log.warn("SmartRecruiters: Response missing 'content' array.")
            return emptyList()
        }

        return contentNode.mapNotNull { node ->
            try {
                mapJob(node)
            } catch (e: Exception) {
                log.error("SmartRecruiters: Failed to normalize job node: ${e.message}", e)
                null
            }
        }
    }

    private fun mapJob(job: JsonNode): NormalizedJob {
        val platformId = job.get("id")?.asText()
            ?: throw IllegalArgumentException("SmartRecruiters job missing required 'id' field")

        val title = job.get("name")?.asText()

        val locationNode = job.path("location")
        val location = locationNode.path("fullLocation").asText(null)
            ?: buildLocationString(locationNode)

        val workModel = when {
            locationNode.path("remote").asBoolean(false) && locationNode.path("hybrid").asBoolean(false) -> "Hybrid"
            locationNode.path("remote").asBoolean(false) -> "Remote"
            locationNode.path("hybrid").asBoolean(false) -> "Hybrid"
            else -> null
        }

        val department     = job.path("department").path("label").asText(null)
        val employmentType = normalizeEmploymentType(job.path("typeOfEmployment").path("label").asText(null))
        val seniorityLevel = normalizeSeniority(job.path("experienceLevel").path("label").asText(null))

        val releasedDate = job.get("releasedDate")?.asText()

        // Build canonical job URL from company identifier
        val companySlug = job.path("company").path("identifier").asText(null)
        val jobUrl = if (companySlug != null) {
            "https://careers.smartrecruiters.com/$companySlug/$platformId"
        } else null

        return NormalizedJob(
            platformId       = platformId,
            source           = "SmartRecruiters",
            title            = title,
            companyName      = "",
            location         = location,
            descriptionHtml  = null, // not available in list response
            descriptionText  = null,
            salaryMin        = null,
            salaryMax        = null,
            salaryCurrency   = null,
            employmentType   = employmentType,
            seniorityLevel   = seniorityLevel,
            workModel        = workModel,
            department       = department,
            postedAt         = releasedDate,
            firstPublishedAt = releasedDate,
            applyUrl         = jobUrl,
            platformUrl      = jobUrl,
            rawPayload       = job.toString()
        )
    }

    private fun buildLocationString(locationNode: JsonNode): String? {
        val parts = listOfNotNull(
            locationNode.path("city").asText(null),
            locationNode.path("region").asText(null),
            locationNode.path("country").asText(null)
        ).filter { it.isNotBlank() }
        return if (parts.isEmpty()) null else parts.joinToString(", ")
    }

    private fun normalizeEmploymentType(raw: String?): String? = when (raw?.lowercase()) {
        "permanent", "full-time", "full time" -> "Full-time"
        "part-time", "part time"              -> "Part-time"
        "contract", "contractor"              -> "Contract"
        "internship", "intern"               -> "Internship"
        "temporary", "temp"                  -> "Temporary"
        else                                 -> raw
    }

    private fun normalizeSeniority(raw: String?): String? = when (raw?.lowercase()) {
        "entry level"                         -> "Entry-level"
        "mid-senior level", "mid senior level" -> "Mid-Senior"
        "senior level", "senior"             -> "Senior"
        "director"                           -> "Director"
        "executive"                          -> "Executive"
        "internship", "intern"               -> "Internship"
        else                                 -> raw
    }
}
