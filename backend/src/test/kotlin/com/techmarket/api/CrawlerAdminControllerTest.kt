package com.techmarket.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.TableResult
import com.google.cloud.bigquery.QueryJobConfiguration
import com.techmarket.persistence.crawler.CrawlerSeedRepository
import com.techmarket.persistence.crawler.CrawlRunRepository
import com.techmarket.service.CrawlLogService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup

class CrawlerAdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var bigQueryProvider: ObjectProvider<BigQuery>
    private lateinit var bigQuery: BigQuery
    private lateinit var crawlerSeedRepository: CrawlerSeedRepository
    private lateinit var crawlRunRepository: CrawlRunRepository
    private lateinit var crawlLogService: CrawlLogService
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        bigQueryProvider = mockk()
        bigQuery = mockk()
        crawlerSeedRepository = mockk()
        crawlRunRepository = mockk()
        crawlLogService = mockk()

        every { bigQueryProvider.ifAvailable } returns bigQuery

        val controller = CrawlerAdminController(
            bigQueryProvider,
            crawlerSeedRepository,
            crawlRunRepository,
            crawlLogService,
            objectMapper,
            "test-dataset",
            "http://localhost:8083"
        )
        mockMvc = standaloneSetup(controller).build()
    }

    @Test
    fun `listCompanies returns warning when bigQuery is null`() {
        every { bigQueryProvider.ifAvailable } returns null
        // Re-create controller to pick up null
        val controller = CrawlerAdminController(
            bigQueryProvider,
            crawlerSeedRepository,
            crawlRunRepository,
            crawlLogService,
            objectMapper,
            "test-dataset",
            "http://localhost:8083"
        )
        val mvc = standaloneSetup(controller).build()

        mvc.perform(get("/api/admin/crawler/companies"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.warning").value("BigQuery is unavailable in this profile"))
    }

    @Test
    fun `listCompanies applies seedStatus filter post-join`() {
        val tableResult = mockk<TableResult>()
        val row1 = mockk<FieldValueList>()
        val row2 = mockk<FieldValueList>()
        
        every { bigQuery.query(any()) } returns tableResult
        every { tableResult.iterateAll() } returns listOf(row1, row2)

        // Mock row1
        every { row1.get("companyId").stringValue } returns "comp1"
        every { row1.get("name").stringValue } returns "Company 1"
        every { row1.get("logoUrl").isNull } returns true
        every { row1.get("hqCountry").isNull } returns true
        every { row1.get("verificationLevel").isNull } returns true
        every { row1.get("employeesCount").isNull } returns true

        // Mock row2
        every { row2.get("companyId").stringValue } returns "comp2"
        every { row2.get("name").stringValue } returns "Company 2"
        every { row2.get("logoUrl").isNull } returns true
        every { row2.get("hqCountry").isNull } returns true
        every { row2.get("verificationLevel").isNull } returns true
        every { row2.get("employeesCount").isNull } returns true

        // Mock total count query
        val totalResult = mockk<TableResult>()
        val totalRow = mockk<FieldValueList>()
        every { totalRow.get("cnt").longValue } returns 2
        every { totalResult.iterateAll() } returns listOf(totalRow)
        // This is a bit tricky because the same query mock might be used for total count if not careful.
        // But since we are mocking bigQuery.query(any()), we can use a sequence or matchers.
        every { bigQuery.query(match { it.query.contains("COUNT(*)") }) } returns totalResult

        every { crawlerSeedRepository.findAggregatedByCompanyIds(any()) } returns mapOf(
            "comp1" to mockk(relaxed = true) { every { seedStatus } returns "ACTIVE" },
            "comp2" to mockk(relaxed = true) { every { seedStatus } returns "STALE" }
        )

        // Test filtering for ACTIVE
        mockMvc.perform(get("/api/admin/crawler/companies").param("seedStatus", "ACTIVE"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].companyId").value("comp1"))
    }

    @Test
    fun `listRuns returns data from repository`() {
        every { crawlRunRepository.findAll(any(), any(), any()) } returns listOf(
            mockk(relaxed = true) {
                every { runId } returns "run1"
                every { companyId } returns "comp1"
                every { isTargeted } returns true
                every { status } returns "COMPLETED"
            }
        )

        mockMvc.perform(get("/api/admin/crawler/runs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].runId").value("run1"))
    }
}
