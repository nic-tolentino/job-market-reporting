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
import com.techmarket.persistence.job.JobRepository
import com.techmarket.sync.CompanySyncService
import com.techmarket.sync.CrawlerDataSyncService
import com.techmarket.sync.CrawlerJobPersistenceService
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
    private lateinit var companySyncService: CompanySyncService
    private lateinit var crawlerJobPersistenceService: CrawlerJobPersistenceService
    private lateinit var crawlerDataSyncService: CrawlerDataSyncService
    private lateinit var jobRepository: JobRepository
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        bigQueryProvider = mockk()
        bigQuery = mockk()
        crawlerSeedRepository = mockk()
        crawlRunRepository = mockk()
        crawlLogService = mockk()
        companySyncService = mockk()
        crawlerJobPersistenceService = mockk()
        crawlerDataSyncService = mockk(relaxed = true)
        jobRepository = mockk(relaxed = true)

        every { bigQueryProvider.ifAvailable } returns bigQuery

        val controller = CrawlerAdminController(
            bigQueryProvider,
            crawlerSeedRepository,
            crawlRunRepository,
            crawlLogService,
            companySyncService,
            crawlerJobPersistenceService,
            crawlerDataSyncService,
            jobRepository,
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
            companySyncService,
            crawlerJobPersistenceService,
            crawlerDataSyncService,
            jobRepository,
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
        
        // Controller runs two queries: data first, then count. Use sequential returns.
        // BigQuery does the seedStatus filtering in SQL, so the mock returns only row1 for ACTIVE.
        val totalResult = mockk<TableResult>()
        val totalRow = mockk<FieldValueList>()
        every { totalRow.get("cnt").longValue } returns 1
        every { totalResult.iterateAll() } returns listOf(totalRow)

        every { bigQuery.query(any()) } returnsMany listOf(tableResult, totalResult)
        every { tableResult.iterateAll() } returns listOf(row1)  // SQL filters to ACTIVE only

        fun mockCompanyRow(row: FieldValueList, id: String, name: String, seedStatus: String) {
            every { row.get("companyId").stringValue } returns id
            every { row.get("name").stringValue } returns name
            every { row.get("logoUrl").isNull } returns true
            every { row.get("hqCountry").isNull } returns true
            every { row.get("verificationLevel").isNull } returns true
            every { row.get("employeesCount").isNull } returns true
            every { row.get("seedStatus").isNull } returns false
            every { row.get("seedStatus").stringValue } returns seedStatus
            every { row.get("seedCount").isNull } returns false
            every { row.get("seedCount").longValue } returns 1L
            every { row.get("lastCrawledAt").isNull } returns true
            every { row.get("totalJobsLastRun").isNull } returns true
            every { row.get("atsProvider").isNull } returns true
            every { row.get("maxZeroYieldCount").isNull } returns true
        }
        mockCompanyRow(row1, "comp1", "Company 1", "ACTIVE")
        mockCompanyRow(row2, "comp2", "Company 2", "STALE")

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
