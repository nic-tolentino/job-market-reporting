package com.techmarket.sync.ats.greenhouse

import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.sync.ats.AtsNormalizer
import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.util.HtmlUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class GreenhouseNormalizer : AtsNormalizer {

    private val log = LoggerFactory.getLogger(GreenhouseNormalizer::class.java)

    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        val jobsNode = rawData.get("jobs")
        if (jobsNode == null || !jobsNode.isArray) {
            log.warn("Greenhouse: Response missing 'jobs' array or is empty.")
            return emptyList()
        }

        return jobsNode.mapNotNull { jobNode ->
            try {
                mapJob(jobNode)
            } catch (e: Exception) {
                log.error("Greenhouse: Failed to normalize job node: ${e.message}", e)
                null
            }
        }
    }

    private fun mapJob(job: JsonNode): NormalizedJob {
        val platformId =
                job.get("id")?.asText()
                        ?: throw IllegalArgumentException(
                                "Greenhouse job missing required 'id' field"
                        )
        val title = job.get("title")?.asText()
        val companyName = "" // Will be filled by AtsJobDataMapper or during sync flow

        // Location prioritization: office location string is usually more precise than board
        // location
        val locationName = job.path("location").path("name").asText(null)
        val officeLocation = job.path("offices").path(0).path("location").asText(null)
        val finalLocation = officeLocation ?: locationName

        val content = job.get("content")?.asText()
        val department = job.path("departments").path(0).path("name").asText(null)
        val updatedAt = job.get("updated_at")?.asText()
        val publishedAt = job.get("first_published_at")?.asText()
        val url = job.get("absolute_url")?.asText()

        return NormalizedJob(
                platformId = platformId,
                source = "Greenhouse",
                title = title,
                companyName = companyName,
                location = finalLocation,
                descriptionHtml = content,
                descriptionText = HtmlUtils.stripHtml(content),
                salaryMin = null,
                salaryMax = null,
                salaryCurrency = null,
                employmentType = null,
                seniorityLevel = null,
                workModel = null,
                department = department,
                postedAt = updatedAt,
                firstPublishedAt = publishedAt,
                applyUrl = url,
                platformUrl = url,
                rawPayload = job.toString()
        )
    }
}
