package com.techmarket.sync.ats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.crawler.CrawlRunRecord
import com.techmarket.persistence.crawler.CrawlRunRepository
import com.techmarket.persistence.crawler.CrawlerSeedRecord
import com.techmarket.persistence.crawler.CrawlerSeedRepository
import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.service.CrawlLogService
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
    val extractionConfidence: Double,
    val extractionStats: ExtractionStats? = null,
    val pagination_pattern: String? = null,
    val atsDirectUrl: String? = null,
    val status: String? = "ACTIVE",
    val errorMessage: String? = null
)

data class ExtractionStats(
    val jobsRaw: Int,
    val jobsValid: Int,
    val jobsTech: Int
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
    private val crawlerSeedRepository: CrawlerSeedRepository,
    private val crawlRunRepository: CrawlRunRepository,
    private val crawlLogService: CrawlLogService,
    @Value("\${crawler.service.url}") private val crawlerUrl: String
) : AtsClient {
    
    private val log = LoggerFactory.getLogger(CrawlerClient::class.java)
    
    /**
     * Fetches jobs from the crawler service for a given company.
     * 
     * @param identifier The company ID to crawl
     * @return Raw JSON response from the crawler service
     */
    override fun fetchJobs(identifier: String): String {
        // Try to load targeted seed from BigQuery first
        val seeds = try {
            crawlerSeedRepository.findByCompanyId(identifier)
        } catch (e: Exception) {
            log.warn("Failed to load seeds from BigQuery for $identifier, falling back to discovery: ${e.message}")
            emptyList()
        }

        val targetedSeed = seeds.find { it.status == "ACTIVE" } ?: seeds.firstOrNull()
        val config = loadCrawlConfig(identifier, targetedSeed)
        
        val request = CrawlRequest(
            companyId = identifier,
            url = config.careersUrl,
            crawlConfig = CrawlConfig(
                maxPages = if (targetedSeed != null) 30 else 5,
                followJobLinks = true,
                timeout = 120000 // 2 minutes for deep crawls
            )
        )
        
        log.info("Crawling company: {} with URL: {} (Targeted: {})", identifier, config.careersUrl, targetedSeed != null)
        crawlLogService.log(identifier, "INFO", "Starting targeted crawl for $identifier at ${config.careersUrl}")
        
        val uri = UriComponentsBuilder
            .fromHttpUrl(crawlerUrl)
            .path("/crawl")
            .build()
            .toUri()
        
        val responseJson = try {
            restTemplate.postForObject(uri, request, String::class.java)
        } catch (e: Exception) {
            log.error("Crawler service request failed for {}: {}", identifier, e.message)
            crawlLogService.log(identifier, "ERROR", "Crawler service request failed: ${e.message}")
            throw RuntimeException("Crawler service request failed: ${e.message}", e)
        }
        
        if (responseJson == null) {
            crawlLogService.log(identifier, "ERROR", "Crawler returned null for $identifier")
            throw RuntimeException("Crawler returned null for $identifier")
        }

        // PERSISTENCE: Background the persistence writes to not block the sync pipeline
        // (Though in this architecture, the Caller handles the JSON result, so we can persist here)
        try {
            val response = objectMapper.readValue<CrawlResponse>(responseJson)
            crawlLogService.log(identifier, "SUCCESS", "Crawler finished: ${response.jobs.size} jobs found in ${response.crawlMeta.crawlDurationMs}ms")
            persistCrawlResult(response, targetedSeed)
        } catch (e: Exception) {
            log.error("Failed to persist crawl result for $identifier: ${e.message}", e)
            crawlLogService.log(identifier, "WARNING", "Failed to persist results: ${e.message}")
        }

        return responseJson
    }

    private fun persistCrawlResult(response: CrawlResponse, initialSeed: CrawlerSeedRecord?) {
        val meta = response.crawlMeta
        val runId = java.util.UUID.randomUUID().toString()
        val now = java.time.Instant.now().toString()

        val seedUrl = initialSeed?.url ?: response.crawlMeta.atsDirectUrl ?: "" // Fallback

        // 1. Append to crawl_runs
        val runRecord = CrawlRunRecord(
            runId = runId,
            batchId = null, // Set by batch caller if needed
            companyId = response.companyId,
            seedUrl = seedUrl,
            isTargeted = initialSeed != null,
            startedAt = now,
            durationMs = meta.crawlDurationMs.toInt(),
            pagesVisited = meta.pagesVisited,
            jobsRaw = meta.extractionStats?.jobsRaw,
            jobsValid = meta.extractionStats?.jobsValid,
            jobsTech = meta.extractionStats?.jobsTech,
            jobsFinal = response.jobs.size,
            confidenceAvg = meta.extractionConfidence,
            atsProvider = meta.detectedAtsProvider,
            atsIdentifier = meta.detectedAtsIdentifier,
            atsDirectUrl = meta.atsDirectUrl,
            paginationPattern = meta.pagination_pattern,
            status = meta.status ?: "ACTIVE",
            errorMessage = meta.errorMessage,
            modelUsed = meta.extractionModel
        )
        crawlRunRepository.append(runRecord)

        // 2. Upsert to crawler_seeds
        val seedRecord = CrawlerSeedRecord(
            companyId = response.companyId,
            url = seedUrl.ifBlank { "unknown" },
            category = initialSeed?.category ?: "unknown",
            status = meta.status ?: "ACTIVE",
            paginationPattern = meta.pagination_pattern,
            lastKnownJobCount = response.jobs.size,
            lastKnownPageCount = meta.pagesVisited,
            lastCrawledAt = now,
            lastDurationMs = meta.crawlDurationMs.toInt(),
            errorMessage = meta.errorMessage,
            consecutiveZeroYieldCount = if (response.jobs.isEmpty()) {
                (initialSeed?.consecutiveZeroYieldCount ?: 0) + 1
            } else 0,
            atsProvider = meta.detectedAtsProvider,
            atsIdentifier = meta.detectedAtsIdentifier,
            atsDirectUrl = meta.atsDirectUrl
        )
        crawlerSeedRepository.upsert(seedRecord)
    }
    
    /**
     * Loads crawl configuration for a company.
     * Tries common career page patterns to find a working URL if no seed exists.
     */
    private fun loadCrawlConfig(companyId: String, seed: CrawlerSeedRecord? = null): CompanyCrawlConfig {
        if (seed != null) {
            return CompanyCrawlConfig(
                careersUrl = seed.url,
                maxPages = 30
            )
        }

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
