package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.sync.ats.CompanyCrawlConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Manages crawl configuration for companies.
 * Loads from and updates company manifests with crawl metadata.
 * 
 * Note: Currently uses in-memory caching. For production, integrate with
 * company manifest JSON storage (GCS) to persist crawl_config field.
 */
@Service
class CrawlConfigService(
    private val companyRepository: CompanyRepository,
    private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(CrawlConfigService::class.java)
    
    // In-memory cache for crawl configs (production should use manifest storage)
    private val configCache = mutableMapOf<String, CompanyCrawlConfig>()

    /**
     * Loads crawl configuration for a company from manifest.
     * Falls back to auto-discovery if not cached.
     */
    fun loadConfig(companyId: String): CompanyCrawlConfig {
        // Check cache first
        configCache[companyId]?.let { return it }
        
        val company = companyRepository.getCompaniesByIds(listOf(companyId)).firstOrNull()
            ?: throw IllegalArgumentException("Company not found: $companyId")

        // TODO: Load from company manifest crawl_config field when available
        // For now, auto-discover career URL
        
        log.info("No cached crawl config for $companyId, auto-discovering...")
        val discoveredUrl = discoverCareerUrl(company.website)
        
        val newConfig = CompanyCrawlConfig(
            careersUrl = discoveredUrl,
            discoveredAt = Instant.now().toString(),
            discoveryMethod = "auto",
            maxPages = 5
        )

        // Cache the discovered URL
        configCache[companyId] = newConfig
        
        log.debug("Cached crawl config for $companyId: ${newConfig.careersUrl}")
        return newConfig
    }

    /**
     * Saves crawl configuration to company manifest.
     */
    fun saveConfig(companyId: String, config: CompanyCrawlConfig) {
        // Cache in memory
        configCache[companyId] = config
        log.info("Saved crawl config for $companyId: ${config.careersUrl}")
        
        // TODO: Persist to company manifest in GCS when manifest storage is available
    }

    /**
     * Updates crawl metadata after successful crawl.
     */
    fun updateAfterCrawl(
        companyId: String,
        jobsCount: Int,
        quality: Double,
        detectedAtsProvider: String? = null,
        detectedAtsIdentifier: String? = null
    ) {
        val currentConfig = loadConfig(companyId)
        
        val updatedConfig = currentConfig.copy(
            lastCrawlAt = Instant.now().toString(),
            lastCrawlQuality = quality,
            lastCrawlJobsCount = jobsCount
        )

        saveConfig(companyId, updatedConfig)

        log.info(
            "Updated crawl metadata for $companyId: {} jobs, quality={}",
            jobsCount,
            quality
        )
    }

    /**
     * Auto-discovers career page URL by trying common patterns.
     */
    private fun discoverCareerUrl(website: String?): String {
        if (website.isNullOrBlank()) {
            return "https://example.com/careers"
        }

        val baseUrl = website.removeSuffix("/")

        // Common career page patterns
        val patterns = listOf(
            "/careers",
            "/jobs",
            "/about/careers",
            "/work-with-us",
            "/join-us",
            "/opportunities"
        )

        // Try patterns with HEAD requests
        val client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(3))
            .build()

        for (pattern in patterns) {
            val url = "$baseUrl$pattern"
            try {
                val request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI(url))
                    .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(3))
                    .build()

                val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding())

                if (response.statusCode() in 200..399) {
                    log.info("Discovered career URL for $baseUrl: $url")
                    return response.uri().toString()
                }
            } catch (e: Exception) {
                // Try next pattern
            }
        }

        // Fallback to /careers
        return "$baseUrl/careers"
    }
}
