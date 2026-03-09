package com.techmarket.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techmarket.config.ApifyProperties
import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.JobRecord
import com.techmarket.service.CloudTasksService
import com.techmarket.service.JobHealthCheckService
import com.techmarket.sync.CompanySyncService
import com.techmarket.sync.JobDataSyncService
import com.techmarket.sync.TechRoleClassifier
import com.techmarket.util.CloudTasksConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup

class AdminControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var cloudTasksService: CloudTasksService
    private lateinit var apifyProperties: ApifyProperties
    private lateinit var jobRepository: JobRepository
    private lateinit var techRoleClassifier: TechRoleClassifier
    private lateinit var companySyncService: CompanySyncService
    private lateinit var jobDataSyncService: JobDataSyncService
    private lateinit var healthCheckService: JobHealthCheckService

    private val objectMapper = jacksonObjectMapper()
    private val webhookSecret = "admin-test-secret"

    @BeforeEach
    fun setup() {
        cloudTasksService = mockk<CloudTasksService>(relaxed = true)
        apifyProperties = ApifyProperties(
            datasetId = "default-dataset",
            token = "test-token",
            webhookSecret = webhookSecret
        )
        jobRepository = mockk<JobRepository>(relaxed = true)
        techRoleClassifier = mockk<TechRoleClassifier>(relaxed = true)
        companySyncService = mockk<CompanySyncService>(relaxed = true)
        jobDataSyncService = mockk<JobDataSyncService>(relaxed = true)
        healthCheckService = mockk<JobHealthCheckService>(relaxed = true)

        val controller = AdminController(
            cloudTasksService = cloudTasksService,
            apifyProperties = apifyProperties,
            jobRepository = jobRepository,
            techRoleClassifier = techRoleClassifier,
            companySyncService = companySyncService,
            jobDataSyncService = jobDataSyncService,
            healthCheckService = healthCheckService
        )

        mockMvc = standaloneSetup(controller).build()
    }

    @Test
    fun `triggerSync queues task and returns task details`() {
        // Arrange
        every { cloudTasksService.queueSyncTask(any()) } returns "projects/test-project/locations/australia-southeast1/queues/tech-market-sync-queue/tasks/admin-task-123"

        // Act & Assert
        mockMvc.perform(
            post("/api/admin/trigger-sync")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"status\":\"queued\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"taskName\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"correlationId\"")))

        // Verify task was queued with correct parameters
        val payloadSlot = slot<CloudTasksService.SyncTaskPayload>()
        verify { cloudTasksService.queueSyncTask(capture(payloadSlot)) }

        val payload = payloadSlot.captured
        assertEquals("default-dataset", payload.datasetId)
        assertEquals(CloudTasksConstants.Source.MANUAL, payload.source)
        assertEquals(CloudTasksConstants.TriggeredBy.ADMIN, payload.triggeredBy)
        assertNull(payload.country)
        assertNotNull(payload.correlationId)
    }

    @Test
    fun `triggerSync uses datasetId from query param when provided`() {
        // Arrange
        every { cloudTasksService.queueSyncTask(any()) } returns "task-123"

        // Act & Assert
        mockMvc.perform(
            post("/api/admin/trigger-sync?datasetId=custom-dataset-456")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isOk)

        // Verify custom datasetId was used
        val payloadSlot = slot<CloudTasksService.SyncTaskPayload>()
        verify { cloudTasksService.queueSyncTask(capture(payloadSlot)) }

        assertEquals("custom-dataset-456", payloadSlot.captured.datasetId)
    }

    @Test
    fun `triggerSync returns 401 Unauthorized without signature`() {
        // Act & Assert
        mockMvc.perform(
            post("/api/admin/trigger-sync")
        )
            .andExpect(status().isUnauthorized)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("error")))

        // Verify no task was queued
        verify(exactly = 0) { cloudTasksService.queueSyncTask(any()) }
    }

    @Test
    fun `triggerSync returns 401 Unauthorized with wrong signature`() {
        // Act & Assert
        mockMvc.perform(
            post("/api/admin/trigger-sync")
                .header("x-apify-signature", "wrong-secret")
        )
            .andExpect(status().isUnauthorized)

        // Verify no task was queued
        verify(exactly = 0) { cloudTasksService.queueSyncTask(any()) }
    }

    @Test
    fun `triggerSync returns 400 Bad Request when datasetId is not configured`() {
        // Arrange
        apifyProperties = ApifyProperties(
            datasetId = "",
            token = "test-token",
            webhookSecret = webhookSecret
        )

        val controller = AdminController(
            cloudTasksService = cloudTasksService,
            apifyProperties = apifyProperties,
            jobRepository = jobRepository,
            techRoleClassifier = techRoleClassifier,
            companySyncService = companySyncService,
            jobDataSyncService = jobDataSyncService,
            healthCheckService = healthCheckService
        )
        mockMvc = standaloneSetup(controller).build()

        // Act & Assert
        mockMvc.perform(
            post("/api/admin/trigger-sync")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("error")))

        // Verify no task was queued
        verify(exactly = 0) { cloudTasksService.queueSyncTask(any()) }
    }

    @Test
    fun `reprocessJobs calls reprocessHistoricalData and returns success`() {
        // Act & Assert
        mockMvc.perform(
            post("/api/admin/reprocess-jobs")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Historical data reprocessing completed")))

        // Verify reprocess was called
        verify { jobDataSyncService.reprocessHistoricalData() }
    }

    @Test
    fun `reprocessJobs returns 401 Unauthorized without signature`() {
        // Act & Assert
        mockMvc.perform(
            post("/api/admin/reprocess-jobs")
        )
            .andExpect(status().isUnauthorized)

        // Verify reprocess was not called
        verify(exactly = 0) { jobDataSyncService.reprocessHistoricalData() }
    }

    @Test
    fun `reprocessJobs returns 500 on failure`() {
        // Arrange
        every { jobDataSyncService.reprocessHistoricalData() } throws RuntimeException("BigQuery connection failed")

        // Act & Assert
        mockMvc.perform(
            post("/api/admin/reprocess-jobs")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isInternalServerError)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Error:")))
    }

    @Test
    fun `syncCompanies calls companySyncService and returns success`() {
        // Act & Assert
        mockMvc.perform(
            post("/api/admin/sync-companies")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Manual Company Manifest Sync executed")))

        // Verify sync was called
        verify { companySyncService.syncFromManifest() }
    }

    @Test
    fun `syncCompanies returns 401 Unauthorized without signature`() {
        // Act & Assert
        mockMvc.perform(
            post("/api/admin/sync-companies")
        )
            .andExpect(status().isUnauthorized)

        // Verify sync was not called
        verify(exactly = 0) { companySyncService.syncFromManifest() }
    }

    @Test
    fun `syncCompanies returns 500 on failure`() {
        // Arrange
        every { companySyncService.syncFromManifest() } throws RuntimeException("Manifest file not found")

        // Act & Assert
        mockMvc.perform(
            post("/api/admin/sync-companies")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isInternalServerError)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Error:")))
    }

    @Test
    fun `auditClassifier returns classifier audit results`() {
        // Arrange
        val job1 = mockk<JobRecord>(relaxed = true)
        val job2 = mockk<JobRecord>(relaxed = true)
        every { job1.jobId } returns "job-1"
        every { job1.title } returns "Software Engineer"
        every { job1.companyName } returns "Tech Corp"
        every { job1.jobFunction } returns "Engineering"
        every { job1.description } returns "Build software"

        every { job2.jobId } returns "job-2"
        every { job2.title } returns "Sales Manager"
        every { job2.companyName } returns "Sales Corp"
        every { job2.jobFunction } returns "Sales"
        every { job2.description } returns "Sell products"

        every { jobRepository.getAllJobs() } returns listOf(job1, job2)
        every { techRoleClassifier.isTechRole("Software Engineer", "Engineering", "Build software") } returns true
        every { techRoleClassifier.isTechRole("Sales Manager", "Sales", "Sell products") } returns false

        // Act & Assert
        mockMvc.perform(
            post("/api/admin/audit-classifier")
                .header("x-apify-signature", webhookSecret)
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"totalJobs\":2")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"techJobs\":1")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("\"filteredJobs\":1")))
    }

    @Test
    fun `auditClassifier returns 401 Unauthorized without signature`() {
        // Act & Assert
        mockMvc.perform(
            post("/api/admin/audit-classifier")
        )
            .andExpect(status().isUnauthorized)

        // Verify classifier was not called
        verify(exactly = 0) { jobRepository.getAllJobs() }
    }
}
