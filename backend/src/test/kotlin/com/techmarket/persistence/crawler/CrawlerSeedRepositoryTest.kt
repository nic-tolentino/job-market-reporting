package com.techmarket.persistence.crawler

import com.google.cloud.bigquery.*
import com.techmarket.persistence.CrawlerSeedFields
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider

class CrawlerSeedRepositoryTest {

    private lateinit var repository: CrawlerSeedRepository
    private lateinit var bigQuery: BigQuery
    private lateinit var bigQueryProvider: ObjectProvider<BigQuery>

    @BeforeEach
    fun setup() {
        bigQuery = mockk()
        bigQueryProvider = mockk()
        every { bigQueryProvider.ifAvailable } returns bigQuery
        repository = CrawlerSeedRepository(bigQueryProvider, "test-dataset")
    }

    private fun aRecord() = CrawlerSeedRecord(
        companyId = "comp1",
        url = "http://example.com",
        category = "tech-filtered",
        status = "ACTIVE",
        paginationPattern = null,
        lastKnownJobCount = null,
        lastKnownPageCount = null,
        lastCrawledAt = null,
        lastDurationMs = null,
        errorMessage = null,
        atsProvider = null,
        atsIdentifier = null,
        atsDirectUrl = null
    )

    @Test
    fun `upsert calls bigQuery query`() {
        val tableResult = mockk<TableResult>()
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult

        repository.upsert(aRecord())

        val configSlot = slot<QueryJobConfiguration>()
        verify { bigQuery.query(capture(configSlot)) }

        val config = configSlot.captured
        assertTrue(config.query.contains("MERGE `test-dataset.crawler_seeds`"))
        assertEquals("comp1", (config.namedParameters["company_id"] as QueryParameterValue).value)
    }

    /**
     * Regression test: INSERT must name columns explicitly (not use positional VALUES)
     * so that adding a column to the BigQuery table doesn't cause "wrong column count" errors.
     *
     * Each column in CrawlerSeedFields.ALL_COLUMNS must appear in the INSERT clause of the
     * upsert SQL. If a new column is added to the table and ALL_COLUMNS is updated,
     * this test will fail until the INSERT clause is updated to match.
     */
    @Test
    fun `upsert SQL uses explicit named INSERT columns covering all schema fields`() {
        val tableResult = mockk<TableResult>()
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult

        repository.upsert(aRecord())

        val configSlot = slot<QueryJobConfiguration>()
        verify { bigQuery.query(capture(configSlot)) }
        val sql = configSlot.captured.query

        // Must use named column INSERT, not bare INSERT VALUES
        assertTrue(sql.contains("INSERT ("), "INSERT clause must name columns explicitly, not use positional VALUES")
        assertFalse(sql.contains("INSERT VALUES"), "Positional INSERT VALUES is not allowed — use INSERT (cols) VALUES (...)")

        // Every column in the schema must appear in the INSERT clause
        val insertSection = sql.substringAfter("INSERT (").substringBefore(") VALUES")
        val missingColumns = CrawlerSeedFields.ALL_COLUMNS.filter { col -> !insertSection.contains(col) }
        assertTrue(missingColumns.isEmpty()) {
            "These columns from CrawlerSeedFields.ALL_COLUMNS are missing from the INSERT clause: $missingColumns"
        }
    }

    /**
     * Regression test: UPDATE SET must include updated_at so the timestamp stays current on re-upserts.
     */
    @Test
    fun `upsert SQL sets updated_at in both INSERT and UPDATE clauses`() {
        val tableResult = mockk<TableResult>()
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult

        repository.upsert(aRecord())

        val configSlot = slot<QueryJobConfiguration>()
        verify { bigQuery.query(capture(configSlot)) }
        val sql = configSlot.captured.query

        val updateSection = sql.substringAfter("WHEN MATCHED THEN UPDATE SET").substringBefore("WHEN NOT MATCHED")
        assertTrue(updateSection.contains("updated_at"), "UPDATE SET must include updated_at")

        val insertSection = sql.substringAfter("INSERT (").substringBefore(") VALUES")
        assertTrue(insertSection.contains("updated_at"), "INSERT column list must include updated_at")
    }

    @Test
    fun `upsert skips when bigQuery is null`() {
        every { bigQueryProvider.ifAvailable } returns null
        val repoNull = CrawlerSeedRepository(bigQueryProvider, "test-dataset")
        
        repoNull.upsert(mockk(relaxed = true))
        
        verify(exactly = 0) { bigQuery.query(any()) }
    }

    @Test
    fun `findByCompanyId returns list of records`() {
        val tableResult = mockk<TableResult>()
        val row = mockk<FieldValueList>()
        
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns listOf(row)
        
        // Mock row fields
        every { row["company_id"].stringValue } returns "comp1"
        every { row["url"].stringValue } returns "http://url"
        every { row["category"].isNull } returns true
        every { row["status"].isNull } returns true
        every { row["pagination_pattern"].isNull } returns true
        every { row["last_known_job_count"].isNull } returns true
        every { row["last_known_page_count"].isNull } returns true
        every { row["last_crawled_at"].isNull } returns true
        every { row["last_duration_ms"].isNull } returns true
        every { row["error_message"].isNull } returns true
        every { row["consecutive_zero_yield_count"].isNull } returns false
        every { row["consecutive_zero_yield_count"].longValue } returns 0
        every { row["ats_provider"].isNull } returns true
        every { row["ats_identifier"].isNull } returns true
        every { row["ats_direct_url"].isNull } returns true

        val results = repository.findByCompanyId("comp1")

        assertEquals(1, results.size)
        assertEquals("comp1", results[0].companyId)
    }

    @Test
    fun `findAggregatedByCompanyIds priority logic`() {
        val tableResult = mockk<TableResult>()
        val row = mockk<FieldValueList>()
        
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns listOf(row)
        
        every { row["company_id"].stringValue } returns "comp1"
        every { row["seed_status"].isNull } returns false
        every { row["seed_status"].stringValue } returns "ACTIVE"
        every { row["seed_count"].longValue } returns 2
        every { row["last_crawled_at"].isNull } returns true
        every { row["total_jobs_last_run"].longValue } returns 10
        every { row["max_zero_yield_count"].longValue } returns 0
        every { row["ats_provider"].isNull } returns true

        val results = repository.findAggregatedByCompanyIds(listOf("comp1"))

        assertEquals(1, results.size)
        assertEquals("ACTIVE", results["comp1"]?.seedStatus)
        assertEquals(2, results["comp1"]?.seedCount)
    }
}
