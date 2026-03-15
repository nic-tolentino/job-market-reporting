package com.techmarket.persistence.crawler

import com.google.cloud.bigquery.*
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

    @Test
    fun `upsert calls bigQuery query`() {
        val record = CrawlerSeedRecord(
            companyId = "comp1",
            url = "http://example.com",
            category = "ENGINEERING",
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
        val tableResult = mockk<TableResult>()
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult

        repository.upsert(record)

        val configSlot = slot<QueryJobConfiguration>()
        verify { bigQuery.query(capture(configSlot)) }
        
        val config = configSlot.captured
        assertTrue(config.query.contains("MERGE `test-dataset.crawler_seeds`"))
        assertEquals("comp1", (config.namedParameters["company_id"] as QueryParameterValue).value)
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
