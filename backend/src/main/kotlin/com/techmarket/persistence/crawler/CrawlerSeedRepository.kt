package com.techmarket.persistence.crawler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.bigquery.*
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.CrawlerSeedFields
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

/**
 * Manages the crawler_seeds table — upserted after every crawl.
 * One row per (company_id, url) composite key.
 */
@Repository
class CrawlerSeedRepository(
    private val bigQuery: BigQuery,
    @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}") private val datasetName: String
) {
    private val log = LoggerFactory.getLogger(CrawlerSeedRepository::class.java)
    private val mapper = jacksonObjectMapper()
    private val table = BigQueryTables.CRAWLER_SEEDS

    /**
     * MERGE upsert: inserts a new row if (company_id, url) not found, updates if it exists.
     */
    fun upsert(record: CrawlerSeedRecord) {
        val sql = """
            MERGE `$datasetName.$table` AS target
            USING (SELECT
                @company_id AS company_id,
                @url AS url,
                @category AS category,
                @status AS status,
                @pagination_pattern AS pagination_pattern,
                @last_known_job_count AS last_known_job_count,
                @last_known_page_count AS last_known_page_count,
                TIMESTAMP(@last_crawled_at) AS last_crawled_at,
                @last_duration_ms AS last_duration_ms,
                @error_message AS error_message,
                @consecutive_zero_yield_count AS consecutive_zero_yield_count,
                @ats_provider AS ats_provider,
                @ats_identifier AS ats_identifier,
                @ats_direct_url AS ats_direct_url
            ) AS source
            ON target.company_id = source.company_id AND target.url = source.url
            WHEN MATCHED THEN UPDATE SET
                category = source.category,
                status = source.status,
                pagination_pattern = source.pagination_pattern,
                last_known_job_count = source.last_known_job_count,
                last_known_page_count = source.last_known_page_count,
                last_crawled_at = source.last_crawled_at,
                last_duration_ms = source.last_duration_ms,
                error_message = source.error_message,
                consecutive_zero_yield_count = source.consecutive_zero_yield_count,
                ats_provider = source.ats_provider,
                ats_identifier = source.ats_identifier,
                ats_direct_url = source.ats_direct_url
            WHEN NOT MATCHED THEN INSERT VALUES (
                source.company_id, source.url, source.category, source.status,
                source.pagination_pattern, source.last_known_job_count,
                source.last_known_page_count, source.last_crawled_at,
                source.last_duration_ms, source.error_message,
                source.consecutive_zero_yield_count, source.ats_provider,
                source.ats_identifier, source.ats_direct_url
            )
        """.trimIndent()

        val config = QueryJobConfiguration.newBuilder(sql)
            .addNamedParameter("company_id", QueryParameterValue.string(record.companyId))
            .addNamedParameter("url", QueryParameterValue.string(record.url))
            .addNamedParameter("category", QueryParameterValue.string(record.category))
            .addNamedParameter("status", QueryParameterValue.string(record.status))
            .addNamedParameter("pagination_pattern", QueryParameterValue.string(record.paginationPattern))
            .addNamedParameter("last_known_job_count", QueryParameterValue.int64(record.lastKnownJobCount?.toLong()))
            .addNamedParameter("last_known_page_count", QueryParameterValue.int64(record.lastKnownPageCount?.toLong()))
            .addNamedParameter("last_crawled_at", QueryParameterValue.string(record.lastCrawledAt))
            .addNamedParameter("last_duration_ms", QueryParameterValue.int64(record.lastDurationMs?.toLong()))
            .addNamedParameter("error_message", QueryParameterValue.string(record.errorMessage))
            .addNamedParameter("consecutive_zero_yield_count", QueryParameterValue.int64(record.consecutiveZeroYieldCount.toLong()))
            .addNamedParameter("ats_provider", QueryParameterValue.string(record.atsProvider))
            .addNamedParameter("ats_identifier", QueryParameterValue.string(record.atsIdentifier))
            .addNamedParameter("ats_direct_url", QueryParameterValue.string(record.atsDirectUrl))
            .build()

        try {
            bigQuery.query(config)
            log.info("Upserted crawler_seeds for ${record.companyId} / ${record.url}")
        } catch (e: Exception) {
            log.error("Failed to upsert crawler_seeds for ${record.companyId}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Returns all seeds for a given company, ordered by last_crawled_at desc.
     */
    fun findByCompanyId(companyId: String): List<CrawlerSeedRecord> {
        val sql = """
            SELECT * FROM `$datasetName.$table`
            WHERE company_id = @company_id
            ORDER BY last_crawled_at DESC
        """.trimIndent()

        val config = QueryJobConfiguration.newBuilder(sql)
            .addNamedParameter("company_id", QueryParameterValue.string(companyId))
            .build()

        return bigQuery.query(config).iterateAll().map { mapRow(it) }
    }

    /**
     * Returns aggregated seed health per company — one row per company_id.
     * Used for the admin company table.
     */
    fun findAggregatedByCompanyIds(companyIds: List<String>): Map<String, AggregatedSeedHealth> {
        if (companyIds.isEmpty()) return emptyMap()

        val sql = """
            SELECT
                company_id,
                CASE
                    WHEN COUNTIF(status = 'ACTIVE') > 0 THEN 'ACTIVE'
                    WHEN COUNTIF(status = 'STALE') > 0 THEN 'STALE'
                    WHEN COUNTIF(status = 'BLOCKED') > 0 THEN 'BLOCKED'
                    WHEN COUNT(*) > 0 THEN MAX(status)
                    ELSE NULL
                END AS seed_status,
                COUNT(*) AS seed_count,
                MAX(last_crawled_at) AS last_crawled_at,
                SUM(COALESCE(last_known_job_count, 0)) AS total_jobs_last_run,
                MAX(consecutive_zero_yield_count) AS max_zero_yield_count,
                MAX(CASE WHEN status = 'ACTIVE' THEN ats_provider ELSE NULL END) AS ats_provider
            FROM `$datasetName.$table`
            WHERE company_id IN UNNEST(@company_ids)
            GROUP BY company_id
        """.trimIndent()

        val config = QueryJobConfiguration.newBuilder(sql)
            .addNamedParameter(
                "company_ids",
                QueryParameterValue.array(companyIds.toTypedArray(), StandardSQLTypeName.STRING)
            )
            .build()

        return bigQuery.query(config).iterateAll().associate { row ->
            val companyId = row["company_id"].stringValue
            companyId to AggregatedSeedHealth(
                seedStatus = if (!row["seed_status"].isNull) row["seed_status"].stringValue else null,
                seedCount = row["seed_count"].longValue.toInt(),
                lastCrawledAt = if (!row["last_crawled_at"].isNull) row["last_crawled_at"].stringValue else null,
                totalJobsLastRun = row["total_jobs_last_run"].longValue.toInt(),
                maxZeroYieldCount = row["max_zero_yield_count"].longValue.toInt(),
                atsProvider = if (!row["ats_provider"].isNull) row["ats_provider"].stringValue else null,
            )
        }
    }

    private fun mapRow(row: FieldValueList): CrawlerSeedRecord = CrawlerSeedRecord(
        companyId = row[CrawlerSeedFields.COMPANY_ID].stringValue,
        url = row[CrawlerSeedFields.URL].stringValue,
        category = row[CrawlerSeedFields.CATEGORY].takeUnless { it.isNull }?.stringValue,
        status = row[CrawlerSeedFields.STATUS].takeUnless { it.isNull }?.stringValue,
        paginationPattern = row[CrawlerSeedFields.PAGINATION_PATTERN].takeUnless { it.isNull }?.stringValue,
        lastKnownJobCount = row[CrawlerSeedFields.LAST_KNOWN_JOB_COUNT].takeUnless { it.isNull }?.longValue?.toInt(),
        lastKnownPageCount = row[CrawlerSeedFields.LAST_KNOWN_PAGE_COUNT].takeUnless { it.isNull }?.longValue?.toInt(),
        lastCrawledAt = row[CrawlerSeedFields.LAST_CRAWLED_AT].takeUnless { it.isNull }?.stringValue,
        lastDurationMs = row[CrawlerSeedFields.LAST_DURATION_MS].takeUnless { it.isNull }?.longValue?.toInt(),
        errorMessage = row[CrawlerSeedFields.ERROR_MESSAGE].takeUnless { it.isNull }?.stringValue,
        consecutiveZeroYieldCount = row[CrawlerSeedFields.CONSECUTIVE_ZERO_YIELD_COUNT].takeUnless { it.isNull }?.longValue?.toInt() ?: 0,
        atsProvider = row[CrawlerSeedFields.ATS_PROVIDER].takeUnless { it.isNull }?.stringValue,
        atsIdentifier = row[CrawlerSeedFields.ATS_IDENTIFIER].takeUnless { it.isNull }?.stringValue,
        atsDirectUrl = row[CrawlerSeedFields.ATS_DIRECT_URL].takeUnless { it.isNull }?.stringValue,
    )
}

data class AggregatedSeedHealth(
    val seedStatus: String?,
    val seedCount: Int,
    val lastCrawledAt: String?,
    val totalJobsLastRun: Int,
    val maxZeroYieldCount: Int,
    val atsProvider: String?,
)
