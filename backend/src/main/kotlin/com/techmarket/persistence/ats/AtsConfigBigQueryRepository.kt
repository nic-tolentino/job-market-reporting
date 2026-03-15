package com.techmarket.persistence.ats

import com.google.cloud.bigquery.*
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.persistence.AtsConfigFields
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.ensureTableExists
import com.techmarket.persistence.model.CompanyAtsConfig
import com.techmarket.sync.ats.SyncStatus
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class AtsConfigBigQueryRepository(
        bigQueryTemplateProvider: ObjectProvider<BigQueryTemplate>,
        bigQueryProvider: ObjectProvider<BigQuery>,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : AtsConfigRepository {

    private val bigQueryTemplate: BigQueryTemplate? = bigQueryTemplateProvider.ifAvailable
    private val bigQuery: BigQuery? = bigQueryProvider.ifAvailable
    private val log = LoggerFactory.getLogger(AtsConfigBigQueryRepository::class.java)
    private val tableName = BigQueryTables.ATS_CONFIGS

    private fun ensureTable() {
        val schema =
                Schema.of(
                        Field.of(AtsConfigFields.COMPANY_ID, StandardSQLTypeName.STRING),
                        Field.of(AtsConfigFields.ATS_PROVIDER, StandardSQLTypeName.STRING),
                        Field.of(AtsConfigFields.IDENTIFIER, StandardSQLTypeName.STRING),
                        Field.of(AtsConfigFields.ENABLED, StandardSQLTypeName.BOOL),
                        Field.of(AtsConfigFields.LAST_SYNCED_AT, StandardSQLTypeName.TIMESTAMP),
                        Field.of(AtsConfigFields.SYNC_STATUS, StandardSQLTypeName.STRING)
                )
        if (bigQuery == null) {
            log.warn("BigQuery unavailable - skipping ATS config table check")
            return
        }
        bigQuery.ensureTableExists(datasetName, tableName, schema)
    }

    override fun getEnabledConfigs(): List<CompanyAtsConfig> {
        if (bigQuery == null) return emptyList()
        val query = AtsConfigQueries.getSelectAllEnabledSql(datasetName, tableName)
        val queryConfig = QueryJobConfiguration.newBuilder(query).build()

        return try {
            val results = bigQuery.query(queryConfig)
            results.iterateAll().map { AtsConfigMapper.mapRow(it) }
        } catch (e: Exception) {
            log.error("GCP: Failed to fetch enabled ATS configs: ${e.message}", e)
            emptyList()
        }
    }

    override fun getConfig(companyId: String): CompanyAtsConfig? {
        if (bigQuery == null) return null
        val query = AtsConfigQueries.getSelectByCompanyIdSql(datasetName, tableName)
        val queryConfig =
                QueryJobConfiguration.newBuilder(query)
                        .addNamedParameter("companyId", QueryParameterValue.string(companyId))
                        .build()

        return try {
            val results = bigQuery.query(queryConfig)
            results.iterateAll().map { AtsConfigMapper.mapRow(it) }.firstOrNull()
        } catch (e: Exception) {
            log.error("GCP: Failed to fetch ATS config for company $companyId: ${e.message}", e)
            null
        }
    }

    override fun updateSyncStatus(companyId: String, status: SyncStatus, syncedAt: Instant) {
        if (bigQuery == null) {
            log.warn("BigQuery unavailable - cannot update sync status for $companyId")
            return
        }
        val sql =
                """
            UPDATE `$datasetName.$tableName` 
            SET ${AtsConfigFields.SYNC_STATUS} = @status, 
                ${AtsConfigFields.LAST_SYNCED_AT} = @syncedAt
            WHERE ${AtsConfigFields.COMPANY_ID} = @companyId
        """.trimIndent()

        val queryConfig =
                QueryJobConfiguration.newBuilder(sql)
                        .addNamedParameter("status", QueryParameterValue.string(status.name))
                        .addNamedParameter(
                                "syncedAt",
                                QueryParameterValue.timestamp(syncedAt.toString())
                        )
                        .addNamedParameter("companyId", QueryParameterValue.string(companyId))
                        .build()

        try {
            bigQuery.query(queryConfig)
            log.info("GCP: Updated sync status for company $companyId to $status")
        } catch (e: Exception) {
            log.error("GCP: Failed to update sync status for $companyId: ${e.message}", e)
            throw e
        }
    }

    override fun saveConfig(config: CompanyAtsConfig) {
        if (bigQueryTemplate == null) {
            log.warn("BigQueryTemplate unavailable - cannot save ATS config for ${config.companyId}")
            return
        }
        // Upsert pattern: Delete then Insert
        deleteConfig(config.companyId)

        log.info("GCP: Saving ATS config for company ${config.companyId}")
        try {
            bigQueryTemplate
                    .writeJsonStream(tableName, listOf(config.toMap()).byteInputStream())
                    .get(60, TimeUnit.SECONDS)
            log.info("GCP: Successfully saved ATS config for ${config.companyId}")
        } catch (e: Exception) {
            log.error("GCP: Failed to save ATS config for ${config.companyId}: ${e.message}", e)
            throw e
        }
    }

    private fun deleteConfig(companyId: String) {
        if (bigQuery == null) {
            log.warn("BigQuery unavailable - cannot delete ATS config for $companyId")
            return
        }
        val sql =
                "DELETE FROM `$datasetName.$tableName` WHERE ${AtsConfigFields.COMPANY_ID} = @companyId"
        val queryConfig =
                QueryJobConfiguration.newBuilder(sql)
                        .addNamedParameter("companyId", QueryParameterValue.string(companyId))
                        .build()
        bigQuery.query(queryConfig)
    }

    private fun CompanyAtsConfig.toMap(): Map<String, Any?> {
        return mapOf(
                AtsConfigFields.COMPANY_ID to this.companyId,
                AtsConfigFields.ATS_PROVIDER to this.atsProvider.name,
                AtsConfigFields.IDENTIFIER to this.identifier,
                AtsConfigFields.ENABLED to this.enabled,
                AtsConfigFields.LAST_SYNCED_AT to this.lastSyncedAt?.toString(),
                AtsConfigFields.SYNC_STATUS to this.syncStatus.name
        )
    }

    private val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

    private fun List<Map<String, Any?>>.byteInputStream(): java.io.InputStream {
        val jsonString = this.joinToString(separator = "\n") { objectMapper.writeValueAsString(it) }
        return jsonString.byteInputStream()
    }
}
