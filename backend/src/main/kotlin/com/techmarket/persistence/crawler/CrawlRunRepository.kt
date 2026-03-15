package com.techmarket.persistence.crawler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.bigquery.*
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.CrawlRunFields
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.util.concurrent.TimeUnit

/**
 * Manages the crawl_runs table — append-only, one row per crawl execution.
 */
@Repository
class CrawlRunRepository(
    bigQueryProvider: ObjectProvider<BigQuery>,
    bigQueryTemplateProvider: ObjectProvider<BigQueryTemplate>,
    @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}") private val datasetName: String
) {
    private val bigQuery: BigQuery? = bigQueryProvider.ifAvailable
    private val bigQueryTemplate: BigQueryTemplate? = bigQueryTemplateProvider.ifAvailable
    private val log = LoggerFactory.getLogger(CrawlRunRepository::class.java)
    private val mapper = jacksonObjectMapper()
    private val table = BigQueryTables.CRAWL_RUNS

    /**
     * Appends a new crawl run record. Never updates existing rows.
     */
    fun append(record: CrawlRunRecord) {
        val row = mapOf(
            CrawlRunFields.RUN_ID to record.runId,
            CrawlRunFields.BATCH_ID to record.batchId,
            CrawlRunFields.COMPANY_ID to record.companyId,
            CrawlRunFields.SEED_URL to record.seedUrl,
            CrawlRunFields.IS_TARGETED to record.isTargeted,
            CrawlRunFields.STARTED_AT to record.startedAt,
            CrawlRunFields.DURATION_MS to record.durationMs,
            CrawlRunFields.PAGES_VISITED to record.pagesVisited,
            CrawlRunFields.JOBS_RAW to record.jobsRaw,
            CrawlRunFields.JOBS_VALID to record.jobsValid,
            CrawlRunFields.JOBS_TECH to record.jobsTech,
            CrawlRunFields.JOBS_FINAL to record.jobsFinal,
            CrawlRunFields.CONFIDENCE_AVG to record.confidenceAvg,
            CrawlRunFields.ATS_PROVIDER to record.atsProvider,
            CrawlRunFields.ATS_IDENTIFIER to record.atsIdentifier,
            CrawlRunFields.ATS_DIRECT_URL to record.atsDirectUrl,
            CrawlRunFields.PAGINATION_PATTERN to record.paginationPattern,
            CrawlRunFields.STATUS to record.status,
            CrawlRunFields.ERROR_MESSAGE to record.errorMessage,
            CrawlRunFields.MODEL_USED to record.modelUsed,
        ).filterValues { it != null }

        if (bigQueryTemplate == null) {
            log.warn("BigQueryTemplate unavailable — skipping append for ${record.companyId}")
            return
        }

        val json = mapper.writeValueAsString(row) + "\n"
        try {
            bigQueryTemplate.writeJsonStream(table, json.byteInputStream())
                .get(60, TimeUnit.SECONDS)
            log.info("Appended crawl_runs for ${record.companyId} run ${record.runId}")
        } catch (e: Exception) {
            log.error("Failed to append crawl_runs for ${record.companyId}: ${e.message}", e)
            throw e
        }
    }

    /**
     * Returns the last N crawl runs for a company, newest first.
     */
    fun findByCompanyId(companyId: String, limit: Int = 20): List<CrawlRunRecord> {
        val sql = """
            SELECT * FROM `$datasetName.$table`
            WHERE company_id = @company_id
            ORDER BY started_at DESC
            LIMIT @limit
        """.trimIndent()

        val config = QueryJobConfiguration.newBuilder(sql)
            .addNamedParameter("company_id", QueryParameterValue.string(companyId))
            .addNamedParameter("limit", QueryParameterValue.int64(limit.toLong()))
            .build()

        if (bigQuery == null) return emptyList()

        return try {
            bigQuery.query(config).iterateAll().map { mapRow(it) }
        } catch (e: Exception) {
            log.warn("crawl_runs query failed (table may not exist yet): ${e.message}")
            emptyList()
        }
    }

    /**
     * Returns paginated crawl runs across all companies, newest first.
     */
    fun findAll(limit: Int = 50, offset: Int = 0, companyId: String? = null): List<CrawlRunRecord> {
        val whereClause = if (companyId != null) "WHERE company_id = @company_id" else ""
        val sql = """
            SELECT * FROM `$datasetName.$table`
            $whereClause
            ORDER BY started_at DESC
            LIMIT @limit OFFSET @offset
        """.trimIndent()

        val configBuilder = QueryJobConfiguration.newBuilder(sql)
            .addNamedParameter("limit", QueryParameterValue.int64(limit.toLong()))
            .addNamedParameter("offset", QueryParameterValue.int64(offset.toLong()))
        if (companyId != null) {
            configBuilder.addNamedParameter("company_id", QueryParameterValue.string(companyId))
        }

        if (bigQuery == null) return emptyList()

        return try {
            bigQuery.query(configBuilder.build()).iterateAll().map { mapRow(it) }
        } catch (e: Exception) {
            log.warn("crawl_runs query failed (table may not exist yet): ${e.message}")
            emptyList()
        }
    }

    private fun mapRow(row: FieldValueList): CrawlRunRecord = CrawlRunRecord(
        runId = row[CrawlRunFields.RUN_ID].stringValue,
        batchId = row[CrawlRunFields.BATCH_ID].takeUnless { it.isNull }?.stringValue,
        companyId = row[CrawlRunFields.COMPANY_ID].stringValue,
        seedUrl = row[CrawlRunFields.SEED_URL].stringValue,
        isTargeted = row[CrawlRunFields.IS_TARGETED].booleanValue,
        startedAt = row[CrawlRunFields.STARTED_AT].stringValue,
        durationMs = row[CrawlRunFields.DURATION_MS].takeUnless { it.isNull }?.longValue?.toInt(),
        pagesVisited = row[CrawlRunFields.PAGES_VISITED].takeUnless { it.isNull }?.longValue?.toInt(),
        jobsRaw = row[CrawlRunFields.JOBS_RAW].takeUnless { it.isNull }?.longValue?.toInt(),
        jobsValid = row[CrawlRunFields.JOBS_VALID].takeUnless { it.isNull }?.longValue?.toInt(),
        jobsTech = row[CrawlRunFields.JOBS_TECH].takeUnless { it.isNull }?.longValue?.toInt(),
        jobsFinal = row[CrawlRunFields.JOBS_FINAL].takeUnless { it.isNull }?.longValue?.toInt(),
        confidenceAvg = row[CrawlRunFields.CONFIDENCE_AVG].takeUnless { it.isNull }?.doubleValue,
        atsProvider = row[CrawlRunFields.ATS_PROVIDER].takeUnless { it.isNull }?.stringValue,
        atsIdentifier = row[CrawlRunFields.ATS_IDENTIFIER].takeUnless { it.isNull }?.stringValue,
        atsDirectUrl = row[CrawlRunFields.ATS_DIRECT_URL].takeUnless { it.isNull }?.stringValue,
        paginationPattern = row[CrawlRunFields.PAGINATION_PATTERN].takeUnless { it.isNull }?.stringValue,
        status = row[CrawlRunFields.STATUS].stringValue,
        errorMessage = row[CrawlRunFields.ERROR_MESSAGE].takeUnless { it.isNull }?.stringValue,
        modelUsed = row[CrawlRunFields.MODEL_USED].takeUnless { it.isNull }?.stringValue,
    )
}
