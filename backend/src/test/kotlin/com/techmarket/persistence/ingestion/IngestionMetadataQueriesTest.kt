package com.techmarket.persistence.ingestion

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IngestionMetadataQueriesTest {

    @Test
    fun `listManifestsSql with empty whereClause produces valid SQL`() {
        val sql = IngestionMetadataQueries.listManifestsSql("ds", "table", "")
        assertTrue(sql.contains("SELECT * FROM `ds.table`"))
        assertTrue(sql.contains("LIMIT @limit"))
        // Verify no orphaned WHERE
        assertTrue(!sql.contains("WHERE ORDER"))
    }

    @Test
    fun `listManifestsSql with whereClause inserts it correctly`() {
        val where = "WHERE source = @source"
        val sql = IngestionMetadataQueries.listManifestsSql("ds", "table", where)
        assertTrue(sql.contains(where))
        assertTrue(sql.contains("ORDER BY ingested_at DESC"))
    }
}
