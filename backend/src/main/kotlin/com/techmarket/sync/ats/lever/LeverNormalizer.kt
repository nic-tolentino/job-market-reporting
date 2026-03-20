package com.techmarket.sync.ats.lever

import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.sync.ats.AtsNormalizer
import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.util.HtmlUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Normalizes raw Lever API responses into [NormalizedJob] instances.
 *
 * Lever returns a top-level JSON array of posting objects. Key fields:
 *  - `id`                       — stable posting UUID
 *  - `text`                     — job title
 *  - `categories.location`      — primary location string
 *  - `categories.department`    — department (falls back to `categories.team`)
 *  - `categories.commitment`    — employment type (Full-time, Part-time, Contract, Internship)
 *  - `content.descriptionHtml`  — full job description as HTML
 *  - `createdAt`                — epoch millis (Long); used as firstPublishedAt
 *  - `updatedAt`                — epoch millis (Long); used as postedAt
 *  - `hostedUrl`                — canonical job page URL
 *  - `applyUrl`                 — direct apply URL
 *  - `salaryRange`              — optional: min, max, currency, interval
 */
@Component
class LeverNormalizer : AtsNormalizer {

    private val log = LoggerFactory.getLogger(LeverNormalizer::class.java)

    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        // Lever returns a top-level array; handle both array and {"postings": [...]} wrapping
        val postingsNode = when {
            rawData.isArray -> rawData
            rawData.has("postings") && rawData.get("postings").isArray -> rawData.get("postings")
            else -> {
                log.warn("Lever: Unexpected response shape — expected array or {postings:[...]}")
                return emptyList()
            }
        }

        return postingsNode.mapNotNull { node ->
            try {
                mapJob(node)
            } catch (e: Exception) {
                log.error("Lever: Failed to normalize job node: ${e.message}", e)
                null
            }
        }
    }

    private fun mapJob(job: JsonNode): NormalizedJob {
        val platformId =
            job.get("id")?.asText()
                ?: throw IllegalArgumentException("Lever job missing required 'id' field")

        val title = job.get("text")?.asText()

        val categories = job.path("categories")
        val location   = categories.path("location").asText(null)
        val department = categories.path("department").asText(null)
            ?: categories.path("team").asText(null)
        val commitment = categories.path("commitment").asText(null)

        val descriptionHtml = job.path("content").path("descriptionHtml").asText(null)
            ?: job.path("content").path("description").asText(null)

        // Timestamps are epoch milliseconds
        val createdAt = job.get("createdAt")?.asLong()?.let { epochToIso(it) }
        val updatedAt = job.get("updatedAt")?.asLong()?.let { epochToIso(it) }

        val hostedUrl = job.get("hostedUrl")?.asText()
        val applyUrl  = job.get("applyUrl")?.asText()

        // Salary — optional block
        val salaryNode   = job.path("salaryRange")
        val salaryMin    = salaryNode.path("min").asInt(0).takeIf { it > 0 }
        val salaryMax    = salaryNode.path("max").asInt(0).takeIf { it > 0 }
        val salaryCurrency = salaryNode.path("currency").asText(null)

        return NormalizedJob(
            platformId      = platformId,
            source          = "Lever",
            title           = title,
            companyName     = "", // filled by AtsJobDataSyncService after normalization
            location        = location,
            descriptionHtml = descriptionHtml,
            descriptionText = HtmlUtils.stripHtml(descriptionHtml),
            salaryMin       = salaryMin,
            salaryMax       = salaryMax,
            salaryCurrency  = salaryCurrency,
            employmentType  = commitment,
            seniorityLevel  = null,
            workModel       = null,
            department      = department,
            postedAt        = updatedAt ?: createdAt,
            firstPublishedAt = createdAt,
            applyUrl        = applyUrl ?: hostedUrl,
            platformUrl     = hostedUrl,
            rawPayload      = job.toString()
        )
    }

    private fun epochToIso(epochMillis: Long): String =
        Instant.ofEpochMilli(epochMillis).toString()
}
