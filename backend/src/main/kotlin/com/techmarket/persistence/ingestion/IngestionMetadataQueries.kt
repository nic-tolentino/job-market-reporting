package com.techmarket.persistence.ingestion

import com.techmarket.persistence.IngestionMetadataFields

/**
 * Utility for building SQL queries for ingestion metadata.
 * Extracted to allow for consistent SQL safety testing and LIMIT push-down.
 */
object IngestionMetadataQueries {

    fun listManifestsSql(datasetName: String, tableName: String, whereClause: String): String {
        return """
            SELECT * FROM `$datasetName.$tableName`
            $whereClause
            ORDER BY ${IngestionMetadataFields.INGESTED_AT} DESC
            LIMIT @limit
        """.trimIndent()
    }
}
