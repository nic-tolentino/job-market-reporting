package com.techmarket.sync

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableResult
import com.techmarket.sync.QualityScore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Logs crawl metadata to BigQuery for analytics and monitoring.
 */
@Service
class CrawlMetadataLogger(
    private val bigQuery: BigQuery,
    @Value("\${gcp.project-id}") private val projectId: String,
    @Value("\${gcp.bigquery.dataset:crawler_analytics}") private val datasetId: String,
    @Value("\${gcp.bigquery.crawl-table:crawl_results}") private val tableId: String
) {
    private val log = LoggerFactory.getLogger(CrawlMetadataLogger::class.java)

    /**
     * Logs crawl result metadata to BigQuery.
     */
    fun logCrawlResult(
        companyId: String,
        crawlDate: Instant,
        pagesVisited: Int,
        jobsExtracted: Int,
        detectedAtsProvider: String?,
        detectedAtsIdentifier: String?,
        extractionConfidence: Double,
        qualityScore: QualityScore,
        durationMs: Long,
        success: Boolean,
        errorMessage: String? = null
    ) {
        try {
            val insertQuery = """
                INSERT INTO `$projectId.$datasetId.$tableId` (
                    crawl_id,
                    company_id,
                    crawl_date,
                    pages_visited,
                    jobs_extracted,
                    detected_ats_provider,
                    detected_ats_identifier,
                    extraction_confidence,
                    quality_score,
                    quality_tier,
                    anomaly_detected,
                    duration_ms,
                    success,
                    error_message
                ) VALUES (
                    GENERATE_UUID(),
                    '$companyId',
                    TIMESTAMP_MICROS(${crawlDate.toEpochMilli() * 1000}),
                    $pagesVisited,
                    $jobsExtracted,
                    ${wrapNull(detectedAtsProvider)},
                    ${wrapNull(detectedAtsIdentifier)},
                    $extractionConfidence,
                    ${qualityScore.overall},
                    '${qualityScore.getTier()}',
                    ${qualityScore.anomalyDetected},
                    $durationMs,
                    $success,
                    ${wrapNull(errorMessage?.replace("'", "''"))}
                )
            """.trimIndent()

            val config = QueryJobConfiguration.newBuilder(insertQuery).build()
            bigQuery.query(config)

            log.debug("Logged crawl result for $companyId to BigQuery")
        } catch (e: Exception) {
            log.error("Failed to log crawl metadata for $companyId: ${e.message}")
        }
    }

    /**
     * Gets crawl statistics for monitoring dashboard.
     */
    fun getCrawlStats(days: Int = 7): CrawlStats {
        val query = """
            SELECT
                COUNT(*) as total_crawls,
                COUNTIF(success) as successful_crawls,
                AVG(extraction_confidence) as avg_confidence,
                AVG(quality_score) as avg_quality,
                COUNTIF(anomaly_detected) as anomalies,
                AVG(jobs_extracted) as avg_jobs_per_company,
                AVG(duration_ms) as avg_duration_ms
            FROM `$projectId.$datasetId.$tableId`
            WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL $days DAY)
        """.trimIndent()

        return try {
            val result = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            val row = result.iterateAll().firstOrNull() ?: return CrawlStats.empty()

            CrawlStats(
                totalCrawls = row.get("total_crawls").longValue.toInt(),
                successfulCrawls = row.get("successful_crawls").longValue.toInt(),
                successRate = row.get("successful_crawls").longValue.toDouble() / 
                    maxOf(1, row.get("total_crawls").longValue),
                avgConfidence = row.get("avg_confidence").doubleValue,
                avgQuality = row.get("avg_quality").doubleValue,
                anomaliesCount = row.get("anomalies").longValue.toInt(),
                avgJobsPerCompany = row.get("avg_jobs_per_company").doubleValue,
                avgDurationMs = row.get("avg_duration_ms").longValue
            )
        } catch (e: Exception) {
            log.error("Failed to get crawl stats: ${e.message}")
            CrawlStats.empty()
        }
    }

    /**
     * Gets companies with poor extraction quality needing attention.
     */
    fun getLowQualityCompanies(days: Int = 7, threshold: Double = 0.5): List<String> {
        val query = """
            SELECT company_id
            FROM `$projectId.$datasetId.$tableId`
            WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL $days DAY)
            GROUP BY company_id
            HAVING AVG(quality_score) < $threshold
            ORDER BY AVG(quality_score) ASC
            LIMIT 100
        """.trimIndent()

        return try {
            val result = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            result.iterateAll().map { it.get("company_id").stringValue }.toList()
        } catch (e: Exception) {
            log.error("Failed to get low quality companies: ${e.message}")
            emptyList()
        }
    }

    /**
     * Gets ATS provider distribution from crawl data.
     */
    fun getAtsDistribution(days: Int = 30): Map<String, Int> {
        val query = """
            SELECT
                COALESCE(detected_ats_provider, 'UNKNOWN') as ats_provider,
                COUNT(*) as company_count
            FROM `$projectId.$datasetId.$tableId`
            WHERE crawl_date >= TIMESTAMP_SUB(CURRENT_TIMESTAMP(), INTERVAL $days DAY)
            GROUP BY ats_provider
            ORDER BY company_count DESC
        """.trimIndent()

        return try {
            val result = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
            result.iterateAll().associate { 
                it.get("ats_provider").stringValue to it.get("company_count").longValue.toInt()
            }
        } catch (e: Exception) {
            log.error("Failed to get ATS distribution: ${e.message}")
            emptyMap()
        }
    }

    private fun wrapNull(value: String?): String {
        return if (value == null) "NULL" else "'$value'"
    }
}

/**
 * Crawl statistics for monitoring dashboard.
 */
data class CrawlStats(
    val totalCrawls: Int,
    val successfulCrawls: Int,
    val successRate: Double,
    val avgConfidence: Double,
    val avgQuality: Double,
    val anomaliesCount: Int,
    val avgJobsPerCompany: Double,
    val avgDurationMs: Long
) {
    companion object {
        fun empty() = CrawlStats(
            totalCrawls = 0,
            successfulCrawls = 0,
            successRate = 0.0,
            avgConfidence = 0.0,
            avgQuality = 0.0,
            anomaliesCount = 0,
            avgJobsPerCompany = 0.0,
            avgDurationMs = 0
        )
    }
}
