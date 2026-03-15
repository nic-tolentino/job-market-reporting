package com.techmarket.api

import com.techmarket.persistence.ingestion.IngestionMetadataRepository
import com.techmarket.service.CloudTasksService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup
import java.time.Instant

class PipelineAdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var cloudTasksService: CloudTasksService
    private lateinit var ingestionRepository: IngestionMetadataRepository

    @BeforeEach
    fun setup() {
        cloudTasksService = mockk()
        ingestionRepository = mockk()
        val controller = PipelineAdminController(cloudTasksService, ingestionRepository)
        mockMvc = standaloneSetup(controller).build()
    }

    @Test
    fun `getQueueStats returns result from service`() {
        every { cloudTasksService.getQueueMetadata() } returns mapOf("queueName" to "jobs", "tasks" to 10)

        mockMvc.perform(get("/api/admin/pipeline/queue"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.queueName").value("jobs"))
            .andExpect(jsonPath("$.tasks").value(10))
    }

    @Test
    fun `getIngestionHistory returns history data`() {
        every { ingestionRepository.listManifests(any(), any(), any(), any()) } returns listOf(
            mockk(relaxed = true) {
                every { metadataId } returns "ds1"
                every { ingestedAt } returns Instant.parse("2024-01-01T00:00:00Z")
                every { recordCount } returns 100
            }
        )

        mockMvc.perform(get("/api/admin/pipeline/history"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].eventId").value("ds1"))
            .andExpect(jsonPath("$.data[0].count").value(100))
    }

    @Test
    fun `getIngestionHistory returns 500 when repository throws`() {
        every { ingestionRepository.listManifests(any(), any(), any(), any()) } throws RuntimeException("DB Error")

        mockMvc.perform(get("/api/admin/pipeline/history"))
            .andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.error").value("DB Error"))
    }
}
