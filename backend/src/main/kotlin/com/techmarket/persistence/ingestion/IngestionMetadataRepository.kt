package com.techmarket.persistence.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.IngestionMetadataFields
import com.techmarket.persistence.ensureTableExists
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import java.time.Instant
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

/**
 * BigQuery repository for Bronze layer metadata.
 * Stores manifests as queryable rows with pointers to GCS files.
 */
@Repository
class IngestionMetadataRepository(
    private val bigQueryTemplate: BigQueryTemplate,
    private val bigQuery: BigQuery,
    private val objectMapper: ObjectMapper,
    @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
    private val datasetName: String
) {
    private val log = LoggerFactory.getLogger(IngestionMetadataRepository::class.java)
    private val tableName = BigQueryTables.INGESTION_METADATA

    fun ensureTable() {
        val schema = Schema.of(
            Field.of(IngestionMetadataFields.DATASET_ID, StandardSQLTypeName.STRING),
            Field.of(IngestionMetadataFields.SOURCE, StandardSQLTypeName.STRING),
            Field.of(IngestionMetadataFields.INGESTED_AT, StandardSQLTypeName.TIMESTAMP),
            Field.of(IngestionMetadataFields.TARGET_COUNTRY, StandardSQLTypeName.STRING),
            Field.of(IngestionMetadataFields.SCHEMA_VERSION, StandardSQLTypeName.STRING),
            Field.of(IngestionMetadataFields.RECORD_COUNT, StandardSQLTypeName.INT64),
            Field.of(IngestionMetadataFields.FILE_COUNT, StandardSQLTypeName.INT64),
            Field.of(IngestionMetadataFields.UNCOMPRESSED_SIZE_BYTES, StandardSQLTypeName.INT64),
            Field.of(IngestionMetadataFields.COMPRESSED_SIZE_BYTES, StandardSQLTypeName.INT64),
            Field.of(IngestionMetadataFields.COMPRESSION_RATIO, StandardSQLTypeName.FLOAT64),
            Field.of(IngestionMetadataFields.PROCESSING_STATUS, StandardSQLTypeName.STRING),
            Field.newBuilder(IngestionMetadataFields.FILES, StandardSQLTypeName.STRING)
                .setMode(Field.Mode.REPEATED)
                .build(),
            Field.of(IngestionMetadataFields.METADATA_ID, StandardSQLTypeName.STRING)
        )
        bigQuery.ensureTableExists(datasetName, tableName, schema)
    }

    fun saveManifest(manifest: BronzeIngestionManifest) {
        ensureTable()
        log.info("Saving ingestion metadata to BigQuery: datasetId=${manifest.datasetId}")

        try {
            bigQueryTemplate.writeJsonStream(
                tableName,
                listOf(manifest.toMap()).toJsonStream()
            ).get()
            log.info("Successfully saved ingestion metadata: ${manifest.datasetId}")
        } catch (e: Exception) {
            log.error("Failed to save ingestion metadata: ${e.message}", e)
            throw e
        }
    }

    fun getManifest(datasetId: String): BronzeIngestionManifest? {
        ensureTable()
        val query = """
            SELECT * FROM `$datasetName.$tableName`
            WHERE ${IngestionMetadataFields.DATASET_ID} = @datasetId
            LIMIT 1
        """.trimIndent()

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("datasetId", QueryParameterValue.string(datasetId))
            .build()

        return try {
            val results = bigQuery.query(queryConfig)
            results.iterateAll().firstOrNull()?.let { mapRow(it) }
        } catch (e: Exception) {
            log.error("Failed to get manifest for dataset $datasetId: ${e.message}", e)
            null
        }
    }

    fun updateProcessingStatus(datasetId: String, status: ProcessingStatus): Boolean {
        ensureTable()
        val query = """
            UPDATE `$datasetName.$tableName`
            SET ${IngestionMetadataFields.PROCESSING_STATUS} = @status
            WHERE ${IngestionMetadataFields.DATASET_ID} = @datasetId
        """.trimIndent()

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("status", QueryParameterValue.string(status.name))
            .addNamedParameter("datasetId", QueryParameterValue.string(datasetId))
            .build()

        return try {
            bigQuery.query(queryConfig)
            log.info("Updated processing status to $status for dataset $datasetId")
            true
        } catch (e: Exception) {
            log.error("Failed to update processing status for dataset $datasetId: ${e.message}", e)
            false
        }
    }

    fun isDatasetIngested(datasetId: String): Boolean {
        ensureTable()
        val query = """
            SELECT 1 FROM `$datasetName.$tableName`
            WHERE ${IngestionMetadataFields.DATASET_ID} = @datasetId
            LIMIT 1
        """.trimIndent()

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("datasetId", QueryParameterValue.string(datasetId))
            .build()

        return try {
            val results = bigQuery.query(queryConfig)
            results.iterateAll().any()
        } catch (e: Exception) {
            log.error("Failed to check if dataset $datasetId is ingested: ${e.message}", e)
            false
        }
    }

    fun listManifests(
        source: String?,
        fromDate: Instant?,
        toDate: Instant?
    ): List<BronzeIngestionManifest> {
        ensureTable()

        val whereClauses = mutableListOf<String>()
        val parameters = mutableMapOf<String, QueryParameterValue>()

        source?.let {
            whereClauses.add("${IngestionMetadataFields.SOURCE} = @source")
            parameters["source"] = QueryParameterValue.string(it)
        }

        fromDate?.let {
            whereClauses.add("${IngestionMetadataFields.INGESTED_AT} >= @fromDate")
            parameters["fromDate"] = QueryParameterValue.timestamp(it.toEpochMilli() * 1000)
        }

        toDate?.let {
            whereClauses.add("${IngestionMetadataFields.INGESTED_AT} <= @toDate")
            parameters["toDate"] = QueryParameterValue.timestamp(it.toEpochMilli() * 1000)
        }

        val whereClause = if (whereClauses.isNotEmpty()) {
            "WHERE " + whereClauses.joinToString(" AND ")
        } else ""

        val query = """
            SELECT * FROM `$datasetName.$tableName`
            $whereClause
            ORDER BY ${IngestionMetadataFields.INGESTED_AT} DESC
        """.trimIndent()

        val queryConfig = QueryJobConfiguration.newBuilder(query).apply {
            parameters.forEach { (key, value) ->
                addNamedParameter(key, value)
            }
        }.build()

        return try {
            val results = bigQuery.query(queryConfig)
            results.iterateAll().map { mapRow(it) }.toList()
        } catch (e: Exception) {
            log.error("Failed to list manifests: ${e.message}", e)
            emptyList()
        }
    }

    private fun mapRow(row: FieldValueList): BronzeIngestionManifest {
        val timestampMicros = getField(row, IngestionMetadataFields.INGESTED_AT).timestampValue
        val filesValue = getField(row, IngestionMetadataFields.FILES)
        val files = filesValue.repeatedValue.map { it.stringValue }

        return BronzeIngestionManifest(
            datasetId = getField(row, IngestionMetadataFields.DATASET_ID).stringValue,
            source = getField(row, IngestionMetadataFields.SOURCE).stringValue,
            ingestedAt = Instant.ofEpochMilli(timestampMicros / 1000),
            targetCountry = if (!getField(row, IngestionMetadataFields.TARGET_COUNTRY).isNull) {
                getField(row, IngestionMetadataFields.TARGET_COUNTRY).stringValue
            } else null,
            schemaVersion = getField(row, IngestionMetadataFields.SCHEMA_VERSION).stringValue ?: "1.0",
            recordCount = getField(row, IngestionMetadataFields.RECORD_COUNT).longValue.toInt(),
            fileCount = getField(row, IngestionMetadataFields.FILE_COUNT).longValue.toInt(),
            uncompressedSizeBytes = getField(row, IngestionMetadataFields.UNCOMPRESSED_SIZE_BYTES).longValue,
            compressedSizeBytes = getField(row, IngestionMetadataFields.COMPRESSED_SIZE_BYTES).longValue,
            compressionRatio = getField(row, IngestionMetadataFields.COMPRESSION_RATIO).doubleValue,
            processingStatus = ProcessingStatus.valueOf(
                getField(row, IngestionMetadataFields.PROCESSING_STATUS).stringValue ?: "PENDING"
            ),
            files = files,
            metadataId = if (!getField(row, IngestionMetadataFields.METADATA_ID).isNull) {
                getField(row, IngestionMetadataFields.METADATA_ID).stringValue
            } else null
        )
    }

    private fun BronzeIngestionManifest.toMap(): Map<String, Any?> {
        return mapOf(
            IngestionMetadataFields.DATASET_ID to this.datasetId,
            IngestionMetadataFields.SOURCE to this.source,
            IngestionMetadataFields.INGESTED_AT to this.ingestedAt.toEpochMilli() * 1000,
            IngestionMetadataFields.TARGET_COUNTRY to this.targetCountry,
            IngestionMetadataFields.SCHEMA_VERSION to this.schemaVersion,
            IngestionMetadataFields.RECORD_COUNT to this.recordCount,
            IngestionMetadataFields.FILE_COUNT to this.fileCount,
            IngestionMetadataFields.UNCOMPRESSED_SIZE_BYTES to this.uncompressedSizeBytes,
            IngestionMetadataFields.COMPRESSED_SIZE_BYTES to this.compressedSizeBytes,
            IngestionMetadataFields.COMPRESSION_RATIO to this.compressionRatio,
            IngestionMetadataFields.PROCESSING_STATUS to this.processingStatus.name,
            IngestionMetadataFields.FILES to this.files,
            IngestionMetadataFields.METADATA_ID to this.metadataId
        )
    }

    private fun List<Map<String, Any?>>.toJsonStream(): java.io.InputStream {
        val jsonString = this.joinToString(separator = "\n") {
            objectMapper.writeValueAsString(it)
        }
        return jsonString.byteInputStream()
    }

    private fun getField(row: FieldValueList, fieldName: String): com.google.cloud.bigquery.FieldValue {
        return row[fieldName]
    }
}
