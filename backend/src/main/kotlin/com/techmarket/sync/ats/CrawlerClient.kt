package com.techmarket.sync.ats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.sync.ats.model.NormalizedJob
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

/**
 * Company crawl configuration stored in manifest
 */
data class CompanyCrawlConfig(
    val careersUrl: String,
    val discoveredAt: String? = null,
    val discoveryMethod: String? = null,  // "auto", "manual", "google_search"
    val extractionHints: Map<String, String> = emptyMap(),
    val lastCrawlAt: String? = null,
    val lastCrawlQuality: Double? = null,
    val lastCrawlJobsCount: Int? = null,
    val maxPages: Int = 5
)

/**
 * Request payload for the crawler service API
 */
data class CrawlRequest(
    val companyId: String,
    val url: String,
    val crawlConfig: CrawlConfig? = null
)

data class CrawlConfig(
    val maxPages: Int? = null,
    val followJobLinks: Boolean? = null,
    val extractionPrompt: String? = null,
    val knownAtsProvider: String? = null,
    val timeout: Int? = null,
    val extractionHints: Map<String, String> = emptyMap()
)

/**
 * Response from the crawler service API
 */
data class CrawlResponse(
    val companyId: String,
    val crawlMeta: CrawlMeta,
    val jobs: List<CrawlerJob>
)

data class CrawlMeta(
    val pagesVisited: Int,
    val totalJobsFound: Int,
    val detectedAtsProvider: String?,
    val detectedAtsIdentifier: String?,
    val crawlDurationMs: Long,
    val extractionModel: String,
    val extractionConfidence: Double
)

data class CrawlerJob(
    val platformId: String,
    val source: String,
    val title: String,
    val companyName: String,
    val location: String?,
    val descriptionHtml: String?,
    val descriptionText: String?,
    val salaryMin: Int?,
    val salaryMax: Int?,
    val salaryCurrency: String?,
    val employmentType: String?,
    val seniorityLevel: String?,
    val workModel: String?,
    val department: String?,
    val postedAt: String?,
    val applyUrl: String?,
    val platformUrl: String?
)

/**
 * Client for the self-hosted Crawler Service.
 * 
 * Implements [AtsClient] to integrate with the existing ATS sync pipeline.
 * Communicates with the Crawler Service via HTTP POST requests.
 */
@Component
class CrawlerClient(
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
    private val companyRepository: CompanyRepository,
    @Value("\${crawler.service.url:http://localhost:8080}") private val crawlerUrl: String
) : AtsClient {
    
    private val log = LoggerFactory.getLogger(CrawlerClient::class.java)
    
    /**
     * Fetches jobs from the crawler service for a given company.
     * 
     * @param identifier The company ID to crawl
     * @return Raw JSON response from the crawler service
     */
    override fun fetchJobs(identifier: String): String {
        val config = loadCrawlConfig(identifier)
        
        val request = CrawlRequest(
            companyId = identifier,
            url = config.careersUrl,
            crawlConfig = CrawlConfig(
                maxPages = 5,
                followJobLinks = true,
                timeout = 30000
            )
        )
        
        log.info("Crawling company: {} with URL: {}", identifier, config.careersUrl)
        
        val uri = UriComponentsBuilder
            .fromHttpUrl(crawlerUrl)
            .path("/crawl")
            .build()
            .toUri()
        
        val response = try {
            restTemplate.postForObject(uri, request, String::class.java)
        } catch (e: Exception) {
            log.error("Crawler service request failed for {}: {}", identifier, e.message)
            throw RuntimeException("Crawler service request failed: ${e.message}", e)
        }
        
        return response ?: throw RuntimeException("Crawler returned null for $identifier")
    }
    
    /**
     * Loads crawl configuration for a company.
     * Tries common career page patterns to find a working URL.
     */
    private fun loadCrawlConfig(companyId: String): CompanyCrawlConfig {
        // TODO: Load from company manifest with cached career URL
        val company = companyRepository.getCompaniesByIds(listOf(companyId)).firstOrNull()
        
        val baseUrl = company?.website?.removeSuffix("/") ?: "https://$companyId.com"
        
        // Common career page patterns to try
        val careerPathPatterns = listOf(
            "/careers",
            "/jobs",
            "/about/careers",
            "/work-with-us",
            "/join-us",
            "/opportunities",
            "/careers/",
            "/jobs/"
        )
        
        // Try to find a working career page URL
        val careersUrl = findWorkingCareerUrl(baseUrl, careerPathPatterns)
            ?: "$baseUrl/careers"  // Fallback to most common pattern
        
        log.debug("Career URL for $companyId: $careersUrl")
        
        return CompanyCrawlConfig(
            careersUrl = careersUrl,
            maxPages = 5
        )
    }
    
    /**
     * Tries multiple career page patterns and returns the first working URL.
     * Makes lightweight HEAD requests to check availability.
     */
    private fun findWorkingCareerUrl(baseUrl: String, patterns: List<String>): String? {
        val client = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .build()
        
        for (pattern in patterns) {
            val url = "$baseUrl$pattern"
            try {
                val request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI(url))
                    .method("HEAD", java.net.http.HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build()
                
                val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.discarding())
                
                // Consider 2xx and 3xx (redirects) as valid
                if (response.statusCode() in 200..399) {
                    // Final URL after redirects
                    return response.uri().toString()
                }
            } catch (e: Exception) {
                // URL doesn't work, try next pattern
                log.debug("Career URL pattern failed: $url - ${e.message}")
            }
        }

        return null  // No working pattern found
    }
}
