package com.techmarket.persistence.crawler

import com.google.cloud.bigquery.*
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import java.util.concurrent.CompletableFuture

class CrawlRunRepositoryTest {

    private lateinit var repository: CrawlRunRepository
    private lateinit var bigQuery: BigQuery
    private lateinit var bigQueryTemplate: BigQueryTemplate
    private lateinit var bigQueryProvider: ObjectProvider<BigQuery>
    private lateinit var bigQueryTemplateProvider: ObjectProvider<BigQueryTemplate>

    @BeforeEach
    fun setup() {
        bigQuery = mockk()
        bigQueryTemplate = mockk()
        bigQueryProvider = mockk()
        bigQueryTemplateProvider = mockk()
        
        every { bigQueryProvider.ifAvailable } returns bigQuery
        every { bigQueryTemplateProvider.ifAvailable } returns bigQueryTemplate
        
        repository = CrawlRunRepository(bigQueryProvider, bigQueryTemplateProvider, "test-dataset")
    }

    @Test
    fun `append calls writeJsonStream when template is available`() {
        val record = CrawlRunRecord(
            runId = "run1",
            batchId = null,
            companyId = "comp1",
            seedUrl = "http://seed",
            isTargeted = true,
            startedAt = "2024-01-01T00:00:00Z",
            durationMs = null,
            pagesVisited = null,
            jobsRaw = null,
            jobsValid = null,
            jobsTech = null,
            jobsFinal = null,
            confidenceAvg = null,
            atsProvider = null,
            atsIdentifier = null,
            atsDirectUrl = null,
            paginationPattern = null,
            status = "COMPLETED",
            errorMessage = null,
            modelUsed = null
        )
        
        every { bigQueryTemplate.writeJsonStream(any(), any()) } returns CompletableFuture.completedFuture(null)

        repository.append(record)

        verify { bigQueryTemplate.writeJsonStream("crawl_runs", any()) }
    }

    @Test
    fun `findAll with null companyId omits WHERE clause`() {
        val tableResult = mockk<TableResult>()
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns emptyList()

        repository.findAll(companyId = null)

        val configSlot = slot<QueryJobConfiguration>()
        verify { bigQuery.query(capture(configSlot)) }
        assertFalse(configSlot.captured.query.contains("WHERE company_id"))
    }

    @Test
    fun `findAll with companyId includes WHERE clause`() {
        val tableResult = mockk<TableResult>()
        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns emptyList()

        repository.findAll(companyId = "comp1")

        val configSlot = slot<QueryJobConfiguration>()
        verify { bigQuery.query(capture(configSlot)) }
        assertTrue(configSlot.captured.query.contains("WHERE company_id = @company_id"))
        assertEquals("comp1", (configSlot.captured.namedParameters["company_id"] as QueryParameterValue).value)
    }

    @Test
    fun `findByCompanyId swallows exception and returns empty list`() {
        every { bigQuery.query(any<QueryJobConfiguration>()) } throws RuntimeException("Table not found")

        val results = repository.findByCompanyId("comp1")

        assertTrue(results.isEmpty())
    }
}
