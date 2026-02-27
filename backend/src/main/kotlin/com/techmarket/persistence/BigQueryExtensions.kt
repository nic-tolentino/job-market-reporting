package com.techmarket.persistence

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardTableDefinition
import com.google.cloud.bigquery.TableId
import com.google.cloud.bigquery.TableInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private val log: Logger = LoggerFactory.getLogger("BigQueryExtensions")

internal fun BigQuery.ensureTableExists(datasetName: String, tableName: String, schema: Schema) {
    val tableId = TableId.of(datasetName, tableName)
    val table = this.getTable(tableId)
    if (table == null) {
        log.info("GCP: Table $tableName not found in dataset $datasetName. Creating now...")
        val tableDefinition = StandardTableDefinition.of(schema)
        val tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build()
        this.create(tableInfo)
        log.info("GCP: Table $tableName created successfully.")
    }
}
