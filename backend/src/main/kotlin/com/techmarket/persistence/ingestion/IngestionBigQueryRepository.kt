package com.techmarket.persistence.ingestion

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.IngestionFields
import com.techmarket.persistence.ensureTableExists
import com.techmarket.persistence.model.RawIngestionRecord
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

/**
 * Implementation of [IngestionRepository] for Google BigQuery. Manages the Bronze Layer
 * 'raw_ingestions' table, which acts as the immutable source of truth.
 */
@Repository
class IngestionBigQueryRepository(
        private val bigQueryTemplate: BigQueryTemplate,
        private val bigQuery: BigQuery,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : IngestionRepository {

    private val log = LoggerFactory.getLogger(IngestionBigQueryRepository::class.java)
    private val ingestionsTableName = BigQueryTables.INGESTIONS

    private fun ensureTable() {
        val schema =
                Schema.of(
                        Field.of(IngestionFields.ID, StandardSQLTypeName.STRING),
                        Field.of(IngestionFields.SOURCE, StandardSQLTypeName.STRING),
                        Field.of(IngestionFields.INGESTED_AT, StandardSQLTypeName.TIMESTAMP),
                        Field.of(IngestionFields.RAW_PAYLOAD, StandardSQLTypeName.STRING)
                )
        bigQuery.ensureTableExists(datasetName, ingestionsTableName, schema)
    }

    override fun saveRawIngestions(records: List<RawIngestionRecord>) {
        if (records.isEmpty()) return
        ensureTable()

        log.info(
                "GCP: Streaming \${records.size} raw ingestion records to BigQuery table: \$ingestionsTableName"
        )
        try {
            bigQueryTemplate
                    .writeJsonStream(
                            ingestionsTableName,
                            records.map { it.toMap() }.byteInputStream()
                    )
                    .get(60, TimeUnit.SECONDS)
            log.info("GCP: Successfully inserted raw ingestion records to BigQuery.")
        } catch (e: Exception) {
            log.error("GCP: Failed to insert raw ingestion records: \${e.message}", e)
            throw e
        }
    }

    override fun getRawIngestions(): List<RawIngestionRecord> {
        ensureTable()
        log.info("GCP: Fetching all raw ingestions for reprocessing")
        val query = IngestionQueries.getSelectAllSql(datasetName, ingestionsTableName)
        val queryConfig = QueryJobConfiguration.newBuilder(query).build()

        return try {
            val results = bigQuery.query(queryConfig)
            results.iterateAll().map { IngestionMapper.mapRow(it) }.toList()
        } catch (e: Exception) {
            log.error("GCP: Failed to fetch raw ingestions: \${e.message}", e)
            emptyList()
        }
    }

    private fun RawIngestionRecord.toMap(): Map<String, Any?> {
        return mapOf(
                IngestionFields.ID to this.id,
                IngestionFields.SOURCE to this.source,
                IngestionFields.INGESTED_AT to this.ingestedAt.toString(),
                IngestionFields.RAW_PAYLOAD to this.rawPayload
        )
    }

    private fun List<Map<String, Any?>>.byteInputStream(): java.io.InputStream {
        val jsonString =
                this.joinToString(separator = "\n") {
                    com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(it)
                }
        return jsonString.byteInputStream()
    }
}
