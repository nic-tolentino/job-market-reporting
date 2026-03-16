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
import com.techmarket.sync.CompanySyncService
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
    private val companySyncService: CompanySyncService,
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
        @RequestParam("sortBy", defaultValue = "name") sortBy: String,
        @RequestParam("sortOrder", defaultValue = "ASC") sortOrder: String,
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

        // Sanitize sortOrder
        val finalSortOrder = if (sortOrder.uppercase() == "DESC") "DESC" else "ASC"
        // Map frontend sort fields to SQL fields
        val sortField = when (sortBy) {
            "name" -> "c.name"
            "seedStatus" -> "s.seed_status"
            "atsProvider" -> "s.ats_provider"
            "lastCrawledAt" -> "s.last_crawled_at"
            "totalJobsLastRun" -> "s.total_jobs_last_run"
            "hqCountry" -> "c.hqCountry"
            "seedCount" -> "s.seed_count"
            else -> "c.name"
        }

        val queryConfig = buildCompanyQuery(
            searchPattern = searchPattern,
            hqCountry = hqCountry,
            seedStatus = seedStatus,
            limit = limit,
            offset = offset,
            sortField = sortField,
            sortOrder = finalSortOrder
        )

        return try {
            val results = bigQuery.query(queryConfig).iterateAll().map { row ->
                mapOf(
                    "companyId" to row.get("companyId").stringValue,
                    "name" to row.get("name").stringValue,
                    "logoUrl" to (row.get("logoUrl").takeUnless { it.isNull }?.stringValue ?: ""),
                    "hqCountry" to (row.get("hqCountry").takeUnless { it.isNull }?.stringValue),
                    "verificationLevel" to (row.get("verificationLevel").takeUnless { it.isNull }?.stringValue),
                    "employeesCount" to (row.get("employeesCount").takeUnless { it.isNull }?.longValue?.toInt()),
                    "seedStatus" to (row.get("seedStatus").takeUnless { it.isNull }?.stringValue),
                    "seedCount" to (row.get("seedCount").takeUnless { it.isNull }?.longValue?.toInt() ?: 0),
                    "lastCrawledAt" to (row.get("lastCrawledAt").takeUnless { it.isNull }?.let {
                        java.time.Instant.ofEpochMilli(it.timestampValue / 1000).toString()
                    }),
                    "totalJobsLastRun" to (row.get("totalJobsLastRun").takeUnless { it.isNull }?.longValue?.toInt() ?: 0),
                    "atsProvider" to (row.get("atsProvider").takeUnless { it.isNull }?.stringValue),
                    "maxZeroYieldCount" to (row.get("maxZeroYieldCount").takeUnless { it.isNull }?.longValue?.toInt() ?: 0),
                )
            }

            // Count total matching the filters
            val totalSql = buildCompanyCountQuery(searchPattern, hqCountry, seedStatus)
            val total = bigQuery.query(totalSql).iterateAll().firstOrNull()?.get("cnt")?.longValue?.toInt() ?: 0

            ResponseEntity.ok(mapOf(
                "data" to results,
                "page" to page,
                "limit" to limit,
                "total" to total,
            ))
        } catch (e: Exception) {
            log.error("BigQuery query failed for listCompanies: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "BigQuery query failed")))
        }
    }

    // ---------------------------------------------------------------------------
    // GET /api/admin/crawler/companies/{companyId}
    // Company detail: seeds + last 20 crawl runs.
    // ---------------------------------------------------------------------------

    @GetMapping("/companies/{companyId}")
    fun getCompany(
        @PathVariable companyId: String,
    ): ResponseEntity<*> {
        val companyMetadata = bigQuery?.let { bq ->
            val sql = "SELECT name, website, logoUrl FROM `$datasetName.${BigQueryTables.COMPANIES}` WHERE companyId = @companyId LIMIT 1"
            val cfg = QueryJobConfiguration.newBuilder(sql)
                .addNamedParameter("companyId", QueryParameterValue.string(companyId))
                .build()
            bq.query(cfg).iterateAll().firstOrNull()?.let { row ->
                mapOf(
                    "name" to row.get("name").stringValue,
                    "website" to (row.get("website").takeUnless { it.isNull }?.stringValue),
                    "logoUrl" to (row.get("logoUrl").takeUnless { it.isNull }?.stringValue)
                )
            }
        }

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
            "name" to (companyMetadata?.get("name") ?: ""),
            "website" to companyMetadata?.get("website"),
            "logoUrl" to companyMetadata?.get("logoUrl"),
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
            val startedAt = java.time.Instant.now().toString()
            crawlLogService.log(companyId, "INFO", "Starting crawl for $companyId at ${body.url}")

            val resultJson = crawlerServiceRestClient.post()
                .uri("/crawl")
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(crawlPayload))
                .retrieve()
                .body(String::class.java)

            val result = objectMapper.readValue<Map<String, Any>>(resultJson ?: "{}")

            // Persist the crawl run to BigQuery
            @Suppress("UNCHECKED_CAST")
            val meta = result["crawlMeta"] as? Map<String, Any> ?: emptyMap()
            @Suppress("UNCHECKED_CAST")
            val stats = result["extractionStats"] as? Map<String, Any>

            val runId = java.util.UUID.randomUUID().toString()
            val record = CrawlRunRecord(
                runId = runId,
                batchId = "admin-manual",
                companyId = companyId,
                seedUrl = body.url,
                isTargeted = true,
                startedAt = startedAt,
                durationMs = (meta["crawlDurationMs"] as? Number)?.toInt(),
                pagesVisited = (meta["pagesVisited"] as? Number)?.toInt(),
                jobsRaw = (stats?.get("jobsRaw") as? Number)?.toInt(),
                jobsValid = (stats?.get("jobsValid") as? Number)?.toInt(),
                jobsTech = (stats?.get("jobsTech") as? Number)?.toInt(),
                jobsFinal = (meta["totalJobsFound"] as? Number)?.toInt(),
                confidenceAvg = (meta["extractionConfidence"] as? Number)?.toDouble(),
                atsProvider = meta["detectedAtsProvider"] as? String,
                atsIdentifier = meta["detectedAtsIdentifier"] as? String,
                atsDirectUrl = meta["atsDirectUrl"] as? String,
                paginationPattern = meta["pagination_pattern"] as? String,
                status = (meta["status"] as? String) ?: "COMPLETED",
                errorMessage = meta["errorMessage"] as? String,
                modelUsed = meta["extractionModel"] as? String,
            )

            val jobsFinalCount = (meta["totalJobsFound"] as? Number)?.toInt() ?: 0
            val durationSec = ((meta["crawlDurationMs"] as? Number)?.toLong() ?: 0) / 1000
            val pagesCount = (meta["pagesVisited"] as? Number)?.toInt() ?: 0
            val crawlStatus = (meta["status"] as? String) ?: "COMPLETED"
            val crawlError = meta["errorMessage"] as? String

            val logLevel = when {
                crawlError != null -> "ERROR"
                jobsFinalCount == 0 -> "WARNING"
                else -> "SUCCESS"
            }
            val summary = when {
                crawlError != null -> "Crawl failed: $crawlError"
                else -> "Crawl complete — $jobsFinalCount jobs, $pagesCount pages in ${durationSec}s (status: $crawlStatus)"
            }
            crawlLogService.log(companyId, logLevel, summary)

            try {
                crawlRunRepository.append(record)
                log.info("Persisted admin crawl run $runId for $companyId")
            } catch (e: Exception) {
                log.error("Failed to persist crawl run for $companyId: ${e.message}", e)
            }

            // Update the seed health in crawler_seeds
            val existingSeed = try {
                crawlerSeedRepository.findByCompanyId(companyId).find { it.url == body.url }
            } catch (e: Exception) { null }

            val jobsFound = (meta["totalJobsFound"] as? Number)?.toInt() ?: 0
            val zeroYieldCount = if (jobsFound == 0)
                (existingSeed?.consecutiveZeroYieldCount ?: 0) + 1
            else 0

            val updatedSeed = CrawlerSeedRecord(
                companyId = companyId,
                url = body.url,
                category = existingSeed?.category
                    ?: (body.seedData?.get("category") as? String)
                    ?: if (body.isDiscovery == true) "homepage" else "general",
                status = (meta["status"] as? String) ?: existingSeed?.status ?: "ACTIVE",
                paginationPattern = (meta["pagination_pattern"] as? String) ?: existingSeed?.paginationPattern,
                lastKnownJobCount = jobsFound,
                lastKnownPageCount = (meta["pagesVisited"] as? Number)?.toInt(),
                lastCrawledAt = meta["lastCrawledAt"] as? String,
                lastDurationMs = (meta["crawlDurationMs"] as? Number)?.toInt(),
                errorMessage = meta["errorMessage"] as? String,
                consecutiveZeroYieldCount = zeroYieldCount,
                atsProvider = meta["detectedAtsProvider"] as? String ?: existingSeed?.atsProvider,
                atsIdentifier = meta["detectedAtsIdentifier"] as? String ?: existingSeed?.atsIdentifier,
                atsDirectUrl = (meta["atsDirectUrl"] as? String) ?: existingSeed?.atsDirectUrl,
            )

            try {
                crawlerSeedRepository.upsert(updatedSeed)
                log.info("Updated crawler_seeds for $companyId / ${body.url}: ${jobsFound} jobs found")
            } catch (e: Exception) {
                log.error("Failed to update crawler_seeds for $companyId: ${e.message}", e)
            }

            ResponseEntity.ok(result)
        } catch (e: Exception) {
            log.error("Crawl failed for $companyId: ${e.message}", e)
            crawlLogService.log(companyId, "ERROR", "Crawl request failed: ${e.message}")
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
    // POST /api/admin/crawler/sync-manifest
    // Trigger a full company manifest sync (reads data/companies/**/*.json → BigQuery).
    // ---------------------------------------------------------------------------

    @PostMapping("/sync-manifest")
    fun syncManifest(): ResponseEntity<*> {
        log.info("Admin triggered company manifest sync via admin panel")
        return try {
            companySyncService.syncFromManifest()
            ResponseEntity.ok(mapOf("status" to "ok", "message" to "Company manifest sync completed"))
        } catch (e: Exception) {
            log.error("Company manifest sync failed: ${e.message}", e)
            ResponseEntity.internalServerError().body(mapOf("error" to (e.message ?: "Sync failed")))
        }
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
    private fun buildCompanyQuery(
        searchPattern: String?,
        hqCountry: String?,
        seedStatus: String?,
        limit: Int,
        offset: Int,
        sortField: String,
        sortOrder: String
    ): QueryJobConfiguration {
        val sql = buildString {
            append("WITH seed_health AS (")
            append("  SELECT company_id,")
            append("    CASE")
            append("      WHEN COUNTIF(status = 'ACTIVE') > 0 THEN 'ACTIVE'")
            append("      WHEN COUNTIF(status = 'STALE') > 0 THEN 'STALE'")
            append("      WHEN COUNTIF(status = 'BLOCKED') > 0 THEN 'BLOCKED'")
            append("      WHEN COUNT(*) > 0 THEN MAX(status)")
            append("      ELSE NULL")
            append("    END AS seed_status,")
            append("    COUNT(*) AS seed_count,")
            append("    MAX(last_crawled_at) AS last_crawled_at,")
            append("    SUM(COALESCE(last_known_job_count, 0)) AS total_jobs_last_run,")
            append("    MAX(consecutive_zero_yield_count) AS max_zero_yield_count,")
            append("    MAX(CASE WHEN status = 'ACTIVE' THEN ats_provider ELSE NULL END) AS ats_provider")
            append("  FROM `$datasetName.${BigQueryTables.CRAWLER_SEEDS}`")
            append("  GROUP BY company_id")
            append(")")
            append("SELECT c.companyId, c.name, c.logoUrl, c.hqCountry, c.verificationLevel, c.employeesCount,")
            append("  s.seed_status as seedStatus, s.seed_count as seedCount, s.last_crawled_at as lastCrawledAt,")
            append("  s.total_jobs_last_run as totalJobsLastRun, s.max_zero_yield_count as maxZeroYieldCount,")
            append("  s.ats_provider as atsProvider")
            append(" FROM `$datasetName.${BigQueryTables.COMPANIES}` c")
            append(" LEFT JOIN seed_health s ON c.companyId = s.company_id")
            
            val where = mutableListOf<String>()
            if (searchPattern != null) where.add("LOWER(c.name) LIKE @searchPattern")
            if (hqCountry != null) where.add("c.hqCountry = @hqCountry")
            if (seedStatus != null) {
                if (seedStatus == "NONE") {
                    where.add("s.company_id IS NULL")
                } else {
                    where.add("s.seed_status = @seedStatus")
                }
            }
            if (where.isNotEmpty()) append(" WHERE ${where.joinToString(" AND ")}")
            
            append(" ORDER BY $sortField $sortOrder, c.name ASC")
            append(" LIMIT @limit OFFSET @offset")
        }

        val cfg = QueryJobConfiguration.newBuilder(sql)
            .addNamedParameter("limit", QueryParameterValue.int64(limit.toLong()))
            .addNamedParameter("offset", QueryParameterValue.int64(offset.toLong()))
        if (searchPattern != null) cfg.addNamedParameter("searchPattern", QueryParameterValue.string(searchPattern))
        if (hqCountry != null) cfg.addNamedParameter("hqCountry", QueryParameterValue.string(hqCountry))
        if (seedStatus != null && seedStatus != "NONE") cfg.addNamedParameter("seedStatus", QueryParameterValue.string(seedStatus))
        return cfg.build()
    }

    /**
     * Builds the SQL query to count total companies matching base filters.
     * Used for pagination calculation.
     */
    private fun buildCompanyCountQuery(searchPattern: String?, hqCountry: String?, seedStatus: String?): QueryJobConfiguration {
        val sql = buildString {
            if (seedStatus != null) {
                append("WITH seed_health AS (")
                append("  SELECT company_id,")
                append("    CASE")
                append("      WHEN COUNTIF(status = 'ACTIVE') > 0 THEN 'ACTIVE'")
                append("      WHEN COUNTIF(status = 'STALE') > 0 THEN 'STALE'")
                append("      WHEN COUNTIF(status = 'BLOCKED') > 0 THEN 'BLOCKED'")
                append("      WHEN COUNT(*) > 0 THEN MAX(status)")
                append("      ELSE NULL")
                append("    END AS seed_status")
                append("  FROM `$datasetName.${BigQueryTables.CRAWLER_SEEDS}`")
                append("  GROUP BY company_id")
                append(")")
                append("SELECT COUNT(*) AS cnt FROM `$datasetName.${BigQueryTables.COMPANIES}` c")
                append(" LEFT JOIN seed_health s ON c.companyId = s.company_id")
            } else {
                append("SELECT COUNT(*) AS cnt FROM `$datasetName.${BigQueryTables.COMPANIES}` c")
            }

            val where = mutableListOf<String>()
            if (searchPattern != null) where.add("LOWER(c.name) LIKE @searchPattern")
            if (hqCountry != null) where.add("c.hqCountry = @hqCountry")
            if (seedStatus != null) {
                if (seedStatus == "NONE") {
                    where.add("s.company_id IS NULL")
                } else {
                    where.add("s.seed_status = @seedStatus")
                }
            }
            if (where.isNotEmpty()) append(" WHERE ${where.joinToString(" AND ")}")
        }
        val cfg = QueryJobConfiguration.newBuilder(sql)
        if (searchPattern != null) cfg.addNamedParameter("searchPattern", QueryParameterValue.string(searchPattern))
        if (hqCountry != null) cfg.addNamedParameter("hqCountry", QueryParameterValue.string(hqCountry))
        if (seedStatus != null && seedStatus != "NONE") cfg.addNamedParameter("seedStatus", QueryParameterValue.string(seedStatus))
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
