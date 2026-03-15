package com.techmarket.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.crawler.AggregatedSeedHealth
import com.techmarket.persistence.crawler.CrawlRunRecord
import com.techmarket.persistence.crawler.CrawlRunRepository
import com.techmarket.persistence.crawler.CrawlerSeedRecord
import com.techmarket.persistence.crawler.CrawlerSeedRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.client.RestClient
import com.techmarket.service.CrawlLogService
import reactor.core.publisher.Flux

import org.springframework.beans.factory.ObjectProvider

/**
 * Admin endpoints for the crawler management panel.
 * Protected by Authorization: Bearer <ADMIN_PANEL_TOKEN>.
 *
 * Distinct from AdminController which uses x-apify-signature and manages sync/reprocess ops.
 * This controller focuses on crawler seed management and crawl history.
 */
@RestController
@RequestMapping("/api/admin/crawler")
class CrawlerAdminController(
    bigQueryProvider: ObjectProvider<BigQuery>,
    private val crawlerSeedRepository: CrawlerSeedRepository,
    private val crawlRunRepository: CrawlRunRepository,
    private val crawlLogService: CrawlLogService,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}") private val datasetName: String,
    @Value("\${crawler.service.url}") private val crawlerServiceUrl: String
) {
    private val bigQuery: BigQuery? = bigQueryProvider.ifAvailable
    private val log = LoggerFactory.getLogger(CrawlerAdminController::class.java)

    init {
        log.info("CrawlerAdminController initialized with: datasetName=$datasetName, crawlerServiceUrl=$crawlerServiceUrl, bigQueryAvailable=${bigQuery != null}")
    }

    private val crawlerServiceRestClient: RestClient by lazy {
        log.info("Creating crawlerServiceRestClient with baseUrl=$crawlerServiceUrl")
        RestClient.builder().baseUrl(crawlerServiceUrl).build()
    }

    @GetMapping(value = ["/logs"], produces = [org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamLogs(): Flux<CrawlLogService.CrawlLogMessage> {
        return crawlLogService.getGlobalStream()
    }

    // ---------------------------------------------------------------------------
    // GET /api/admin/crawler/companies
    // Paginated company list with aggregated seed health from crawler_seeds.
    // ---------------------------------------------------------------------------

    @GetMapping("/companies")
    fun listCompanies(
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("limit", defaultValue = "50") limit: Int,
        @RequestParam("search", required = false) search: String?,
        @RequestParam("seedStatus", required = false) seedStatus: String?,
        @RequestParam("hqCountry", required = false) hqCountry: String?,
    ): ResponseEntity<*> {

        if (bigQuery == null) {
            return ResponseEntity.ok(mapOf(
                "data" to emptyList<Any>(),
                "page" to page,
                "limit" to limit,
                "total" to 0,
                "warning" to "BigQuery is unavailable in this profile"
            ))
        }

        val offset = page * limit
        val searchPattern = search?.let { "%${it.lowercase()}%" }

        val companies = bigQuery.query(buildCompanyQuery(searchPattern, hqCountry, limit, offset)).iterateAll().map { row ->
            mapOf(
                "companyId" to row.get("companyId").stringValue,
                "name" to row.get("name").stringValue,
                "logoUrl" to (row.get("logoUrl").takeUnless { it.isNull }?.stringValue ?: ""),
                "hqCountry" to (row.get("hqCountry").takeUnless { it.isNull }?.stringValue),
                "verificationLevel" to (row.get("verificationLevel").takeUnless { it.isNull }?.stringValue),
                "employeesCount" to (row.get("employeesCount").takeUnless { it.isNull }?.longValue?.toInt()),
            )
        }

        val companyIds = companies.map { it["companyId"] as String }

        // Fetch aggregated seed health for these companies
        val seedHealth: Map<String, AggregatedSeedHealth> = try {
            crawlerSeedRepository.findAggregatedByCompanyIds(companyIds)
        } catch (e: Exception) {
            log.warn("crawler_seeds unavailable — returning companies without seed data: ${e.message}")
            emptyMap()
        }

        // Apply seedStatus filter post-join (simpler than complex SQL)
        var results = companies.map { company ->
            val id = company.get("companyId") as String
            val health = seedHealth.get(id)
            company + mapOf(
                "seedStatus" to health?.seedStatus,
                "seedCount" to (health?.seedCount ?: 0),
                "lastCrawledAt" to health?.lastCrawledAt,
                "totalJobsLastRun" to (health?.totalJobsLastRun ?: 0),
                "atsProvider" to health?.atsProvider,
                "maxZeroYieldCount" to (health?.maxZeroYieldCount ?: 0),
            )
        }

        if (seedStatus != null) {
            results = results.filter { it["seedStatus"] == seedStatus }
        }

        // Count total (simplified — doesn't account for seedStatus filter)
        val totalSql = buildCompanyCountQuery(searchPattern, hqCountry)
        val total = bigQuery.query(totalSql).iterateAll().firstOrNull()?.get("cnt")?.longValue?.toInt() ?: 0

        return ResponseEntity.ok(mapOf(
            "data" to results,
            "page" to page,
            "limit" to limit,
            "total" to total,
        ))
    }

    // ---------------------------------------------------------------------------
    // GET /api/admin/crawler/companies/{companyId}
    // Company detail: seeds + last 20 crawl runs.
    // ---------------------------------------------------------------------------

    @GetMapping("/companies/{companyId}")
    fun getCompany(
        @PathVariable companyId: String,
    ): ResponseEntity<*> {

        val seeds: List<CrawlerSeedRecord> = try {
            crawlerSeedRepository.findByCompanyId(companyId)
        } catch (e: Exception) {
            log.warn("crawler_seeds unavailable for $companyId: ${e.message}")
            emptyList()
        }

        val runs: List<CrawlRunRecord> = try {
            crawlRunRepository.findByCompanyId(companyId, limit = 20)
        } catch (e: Exception) {
            log.warn("crawl_runs unavailable for $companyId: ${e.message}")
            emptyList()
        }

        return ResponseEntity.ok(mapOf(
            "companyId" to companyId,
            "seeds" to seeds.map { seedToMap(it) },
            "recentRuns" to runs.map { runToMap(it) },
        ))
    }

    // ---------------------------------------------------------------------------
    // PUT /api/admin/crawler/seeds
    // Upsert a seed record into crawler_seeds.
    // ---------------------------------------------------------------------------

    @PutMapping("/seeds")
    fun upsertSeed(
        @RequestBody body: UpsertSeedRequest,
    ): ResponseEntity<*> {

        val record = CrawlerSeedRecord(
            companyId = body.companyId,
            url = body.url,
            category = body.category,
            status = body.status,
            paginationPattern = null,
            lastKnownJobCount = null,
            lastKnownPageCount = null,
            lastCrawledAt = null,
            lastDurationMs = null,
            errorMessage = null,
            consecutiveZeroYieldCount = 0,
            atsProvider = null,
            atsIdentifier = null,
            atsDirectUrl = null,
        )

        try {
            crawlerSeedRepository.upsert(record)
        } catch (e: Exception) {
            return ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }

        return ResponseEntity.ok(mapOf("status" to "ok"))
    }

    // ---------------------------------------------------------------------------
    // POST /api/admin/crawler/companies/{companyId}/crawl
    // Trigger a single targeted crawl via the crawler service.
    // ---------------------------------------------------------------------------

    @PostMapping("/companies/{companyId}/crawl")
    fun triggerCrawl(
        @PathVariable companyId: String,
        @RequestBody body: TriggerCrawlRequest,
    ): ResponseEntity<*> {

        log.info("Admin triggering crawl for $companyId at ${body.url}")

        val crawlPayload = mapOf(
            "companyId" to companyId,
            "url" to body.url,
            "crawlConfig" to mapOf(
                "maxPages" to (body.maxPages ?: 15),
                "followJobLinks" to true,
                "isDiscoveryMode" to (body.isDiscovery ?: false),
            ),
            "seedData" to body.seedData,
        ).filterValues { it != null }

        return try {
            val result = crawlerServiceRestClient.post()
                .uri("/crawl")
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(crawlPayload))
                .retrieve()
                .body(String::class.java)

            ResponseEntity.ok(objectMapper.readValue<Map<String, Any>>(result ?: "{}"))
        } catch (e: Exception) {
            log.error("Crawl failed for $companyId: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to e.message))
        }
    }

    // ---------------------------------------------------------------------------
    // GET /api/admin/crawler/runs
    // Paginated crawl run history.
    // ---------------------------------------------------------------------------

    @GetMapping("/runs")
    fun listRuns(
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("limit", defaultValue = "50") limit: Int,
        @RequestParam("companyId", required = false) companyId: String?,
    ): ResponseEntity<*> {

        val runs = try {
            crawlRunRepository.findAll(limit = limit, offset = page * limit, companyId = companyId)
        } catch (e: Exception) {
            log.warn("crawl_runs unavailable: ${e.message}")
            emptyList()
        }

        return ResponseEntity.ok(mapOf(
            "data" to runs.map { runToMap(it) },
            "page" to page,
            "limit" to limit,
        ))
    }

    // ---------------------------------------------------------------------------
    // GET /api/admin/crawler/health
    // ---------------------------------------------------------------------------

    @GetMapping("/health")
    fun health(): ResponseEntity<*> {

        log.info("Health check requested. crawlerServiceUrl: $crawlerServiceUrl")

        val crawlerReachable = try {
            val response = crawlerServiceRestClient.get()
                .uri("/health")
                .retrieve()
                .toEntity(String::class.java)
            
            log.info("Crawler health check status: ${response.statusCode}")
            response.statusCode.is2xxSuccessful
        } catch (e: Exception) {
            log.warn("Crawler health check failed: ${e.message}")
            false
        }

        return ResponseEntity.ok(mapOf(
            "backend" to "ok",
            "crawlerService" to if (crawlerReachable) "ok" else "unreachable",
        ))
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Builds the SQL query to fetch raw company data.
     * 
     * NOTE: Filtering by [seedStatus] is performed in Kotlin AFTER this query returns.
     * This keeps the SQL simple and avoids complex joins across potentially large datasets
     * for the admin-only observability view.
     */
    private fun buildCompanyQuery(search: String?, hqCountry: String?, limit: Int, offset: Int): QueryJobConfiguration {
        val sql = buildString {
            append("SELECT companyId, name, logoUrl, hqCountry, verificationLevel, employeesCount")
            append(" FROM `$datasetName.${BigQueryTables.COMPANIES}`")
            val where = mutableListOf<String>()
            if (search != null) where.add("LOWER(name) LIKE @searchPattern")
            if (hqCountry != null) where.add("hqCountry = @hqCountry")
            if (where.isNotEmpty()) append(" WHERE ${where.joinToString(" AND ")}")
            append(" ORDER BY name ASC LIMIT @limit OFFSET @offset")
        }

        val cfg = QueryJobConfiguration.newBuilder(sql)
            .addNamedParameter("limit", QueryParameterValue.int64(limit.toLong()))
            .addNamedParameter("offset", QueryParameterValue.int64(offset.toLong()))
        if (search != null) cfg.addNamedParameter("searchPattern", QueryParameterValue.string(search))
        if (hqCountry != null) cfg.addNamedParameter("hqCountry", QueryParameterValue.string(hqCountry))
        return cfg.build()
    }

    /**
     * Builds the SQL query to count total companies matching base filters.
     * Used for pagination calculation.
     */
    private fun buildCompanyCountQuery(search: String?, hqCountry: String?): QueryJobConfiguration {
        val sql = buildString {
            append("SELECT COUNT(*) AS cnt FROM `$datasetName.${BigQueryTables.COMPANIES}`")
            val where = mutableListOf<String>()
            if (search != null) where.add("LOWER(name) LIKE @searchPattern")
            if (hqCountry != null) where.add("hqCountry = @hqCountry")
            if (where.isNotEmpty()) append(" WHERE ${where.joinToString(" AND ")}")
        }
        val cfg = QueryJobConfiguration.newBuilder(sql)
        if (search != null) cfg.addNamedParameter("searchPattern", QueryParameterValue.string(search))
        if (hqCountry != null) cfg.addNamedParameter("hqCountry", QueryParameterValue.string(hqCountry))
        return cfg.build()
    }

    private fun seedToMap(s: CrawlerSeedRecord) = mapOf(
        "companyId" to s.companyId,
        "url" to s.url,
        "category" to s.category,
        "status" to s.status,
        "paginationPattern" to s.paginationPattern,
        "lastKnownJobCount" to s.lastKnownJobCount,
        "lastKnownPageCount" to s.lastKnownPageCount,
        "lastCrawledAt" to s.lastCrawledAt,
        "lastDurationMs" to s.lastDurationMs,
        "errorMessage" to s.errorMessage,
        "consecutiveZeroYieldCount" to s.consecutiveZeroYieldCount,
        "atsProvider" to s.atsProvider,
        "atsIdentifier" to s.atsIdentifier,
        "atsDirectUrl" to s.atsDirectUrl,
    )

    private fun runToMap(r: CrawlRunRecord) = mapOf(
        "runId" to r.runId,
        "batchId" to r.batchId,
        "companyId" to r.companyId,
        "seedUrl" to r.seedUrl,
        "isTargeted" to r.isTargeted,
        "startedAt" to r.startedAt,
        "durationMs" to r.durationMs,
        "pagesVisited" to r.pagesVisited,
        "jobsRaw" to r.jobsRaw,
        "jobsValid" to r.jobsValid,
        "jobsTech" to r.jobsTech,
        "jobsFinal" to r.jobsFinal,
        "confidenceAvg" to r.confidenceAvg,
        "atsProvider" to r.atsProvider,
        "atsDirectUrl" to r.atsDirectUrl,
        "paginationPattern" to r.paginationPattern,
        "status" to r.status,
        "errorMessage" to r.errorMessage,
        "modelUsed" to r.modelUsed,
    )
}

data class UpsertSeedRequest(
    val companyId: String,
    val url: String,
    val category: String?,
    val status: String?,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TriggerCrawlRequest(
    val url: String,
    val maxPages: Int? = null,
    val isDiscovery: Boolean? = false,
    val seedData: Map<String, Any>? = null,
)
