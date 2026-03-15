package com.techmarket.sync.ats

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.techmarket.sync.ats.model.NormalizedJob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Normalizer for crawler service responses.
 * 
 * The crawler already returns data in [NormalizedJob] format, so this is mostly
 * a passthrough with validation.
 */
@Component
class CrawlerNormalizer(
    private val objectMapper: ObjectMapper
) : AtsNormalizer {
    
    private val log = LoggerFactory.getLogger(CrawlerNormalizer::class.java)
    
    /**
     * Normalizes crawler response into NormalizedJob list.
     * 
     * @param rawData Raw JSON from crawler service (CrawlResponse format)
     * @return List of normalized jobs, filtered for validity
     */
    override fun normalize(rawData: JsonNode): List<NormalizedJob> {
        try {
            val response = objectMapper.treeToValue(rawData, CrawlResponse::class.java)
            
            log.info(
                "Normalizing crawler response for {}: {} jobs found, confidence: {}",
                response.companyId,
                response.jobs.size,
                response.crawlMeta.extractionConfidence
            )
            
            return response.jobs.mapNotNull { job ->
                try {
                    job.toNormalizedJob()
                } catch (e: Exception) {
                    log.warn("Failed to normalize job from crawler: {}", e.message)
                    null
                }
            }.filter { job ->
                // Filter out jobs without required fields
                if (job.title.isNullOrBlank()) {
                    log.debug("Rejected job without title from crawler")
                    false
                } else {
                    true
                }
            }
        } catch (e: Exception) {
            log.error("Failed to parse crawler response: {}", e.message)
            throw RuntimeException("Failed to parse crawler response: ${e.message}", e)
        }
    }
}

/**
 * Converts a CrawlerJob to NormalizedJob
 */
private fun CrawlerJob.toNormalizedJob(): NormalizedJob {
    return NormalizedJob(
        platformId = this.platformId.ifBlank { generatePlatformId() },
        source = "Crawler",
        title = this.title,
        companyName = this.companyName,
        location = formatLocation(this.location),
        descriptionHtml = this.descriptionHtml,
        descriptionText = this.descriptionText ?: this.descriptionHtml,
        salaryMin = this.salaryMin,
        salaryMax = this.salaryMax,
        salaryCurrency = this.salaryCurrency,
        employmentType = this.employmentType,
        seniorityLevel = this.seniorityLevel,
        workModel = this.workModel,
        department = this.department,
        postedAt = this.postedAt,
        firstPublishedAt = null,
        applyUrl = this.applyUrl,
        platformUrl = this.platformUrl,
        rawPayload = "" // Will be populated by sync service
    )
}

/**
 * Generates a platform ID for crawler jobs
 */
private fun CrawlerJob.generatePlatformId(): String {
    val date = this.postedAt?.replace("-", "")?.take(8) ?: "00000000"
    val slug = this.title.lowercase().replace(Regex("[^a-z0-9]"), "-").take(50)
    return "crawl-$slug-$date"
}

/**
 * Formats location string for consistency
 */
private fun formatLocation(location: String?): String {
    if (location.isNullOrBlank()) return ""
    
    // Clean up common location patterns
    return location.trim()
        .replace(Regex("\\s+"), " ")  // Normalize whitespace
        // Only strip prefix if it's at the start followed by a space and location
        .replace(Regex("^Remote,\\s*"), "Remote - ")
        .replace(Regex("^Hybrid,\\s*"), "Hybrid - ")
}
