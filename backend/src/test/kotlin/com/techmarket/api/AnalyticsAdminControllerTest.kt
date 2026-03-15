package com.techmarket.api

import com.techmarket.api.model.GlobalStatsDto
import com.techmarket.api.model.LandingPageDto
import com.techmarket.persistence.analytics.AnalyticsRepository
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.ingestion.IngestionMetadataRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup

/**
 * Note: These tests use standaloneSetup which bypasses AdminTokenInterceptor.
 * Authentication logic is tested separately in AdminTokenInterceptorTest.
 */
class AnalyticsAdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var analyticsRepository: AnalyticsRepository
    private lateinit var jobRepository: JobRepository
    private lateinit var ingestionRepository: IngestionMetadataRepository

    @BeforeEach
    fun setup() {
        analyticsRepository = mockk()
        jobRepository = mockk()
        ingestionRepository = mockk()
        val controller = AnalyticsAdminController(analyticsRepository, jobRepository, ingestionRepository)
        mockMvc = standaloneSetup(controller).build()
    }

    @Test
    fun `getSummary returns 200 with data`() {
        val landingData = mockk<LandingPageDto>(relaxed = true) {
            every { globalStats.totalVacancies } returns 500
        }
        every { analyticsRepository.getLandingPageData(null) } returns landingData
        every { jobRepository.count() } returns 100
        every { jobRepository.countActive() } returns 80
        every { ingestionRepository.listManifests(any(), any(), any(), any()) } returns listOf(
            mockk(relaxed = true) {
                every { datasetId } returns "ds1"
                every { recordCount } returns 50
            }
        )

        mockMvc.perform(get("/api/admin/analytics/summary"))
            .andExpect(status().isOk)
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalJobsInPersistence").value(100))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.activeJobs").value(80))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.recentIngestions[0].datasetId").value("ds1"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.recentIngestions[0].recordCount").value(50))
    }

    @Test
    fun `getFeedback returns 200`() {
        every { analyticsRepository.getAllFeedback() } returns emptyList()

        mockMvc.perform(get("/api/admin/analytics/feedback"))
            .andExpect(status().isOk)
    }
}
