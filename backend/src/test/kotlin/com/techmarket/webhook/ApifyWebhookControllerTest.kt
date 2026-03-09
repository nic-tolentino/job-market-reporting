package com.techmarket.webhook

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techmarket.config.ApifyProperties
import com.techmarket.service.CloudTasksService
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

class ApifyWebhookControllerTest {

    private lateinit var mockMvc: MockMvc
    private lateinit var cloudTasksService: CloudTasksService
    private lateinit var apifyProperties: ApifyProperties

    private val objectMapper = jacksonObjectMapper()
    private val webhookSecret = "test-webhook-secret"

    @BeforeEach
    fun setup() {
        cloudTasksService = mockk<CloudTasksService>(relaxed = true)
        apifyProperties = ApifyProperties(
            datasetId = "test-dataset",
            token = "test-token",
            webhookSecret = webhookSecret
        )

        val controller = ApifyWebhookController(
            cloudTasksService = cloudTasksService,
            apifyProperties = apifyProperties
        )

        mockMvc = standaloneSetup(controller).build()
    }

    @Test
    fun `handleApifyWebhook queues task and returns 202 Accepted`() {
        // Arrange
        val webhookPayload = mapOf(
            "eventType" to "ACTIVITY",
            "resource" to mapOf("defaultDatasetId" to "dataset-123")
        )

        every { cloudTasksService.queueSyncTask(any()) } returns "projects/test-project/locations/australia-southeast1/queues/tech-market-sync-queue/tasks/task-123"

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/data-changed")
                .header("X-Apify-Webhook-Secret", webhookSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isAccepted)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Data sync queued for background processing")))

        // Verify task was queued
        val payloadSlot = slot<CloudTasksService.SyncTaskPayload>()
        verify { cloudTasksService.queueSyncTask(capture(payloadSlot)) }

        val payload = payloadSlot.captured
        assertEquals("dataset-123", payload.datasetId)
        assertEquals(CloudTasksConstants.Source.APIFY, payload.source)
        assertNull(payload.country)
        assertEquals(CloudTasksConstants.TriggeredBy.WEBHOOK, payload.triggeredBy)
        assertNotNull(payload.correlationId)
    }

    @Test
    fun `handleApifyWebhookNz queues task with NZ country`() {
        // Arrange
        val webhookPayload = mapOf(
            "eventType" to "ACTIVITY",
            "resource" to mapOf("defaultDatasetId" to "nz-dataset")
        )

        every { cloudTasksService.queueSyncTask(any()) } returns "task-nz-123"

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/nz/data-changed")
                .header("X-Apify-Webhook-Secret", webhookSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isAccepted)

        // Verify country is set to NZ
        val payloadSlot = slot<CloudTasksService.SyncTaskPayload>()
        verify { cloudTasksService.queueSyncTask(capture(payloadSlot)) }

        assertEquals("NZ", payloadSlot.captured.country)
    }

    @Test
    fun `handleApifyWebhookAu queues task with AU country`() {
        // Arrange
        val webhookPayload = mapOf(
            "eventType" to "ACTIVITY",
            "resource" to mapOf("defaultDatasetId" to "au-dataset")
        )

        every { cloudTasksService.queueSyncTask(any()) } returns "task-au-123"

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/au/data-changed")
                .header("X-Apify-Webhook-Secret", webhookSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isAccepted)

        // Verify country is set to AU
        val payloadSlot = slot<CloudTasksService.SyncTaskPayload>()
        verify { cloudTasksService.queueSyncTask(capture(payloadSlot)) }

        assertEquals("AU", payloadSlot.captured.country)
    }

    @Test
    fun `handleApifyWebhook returns 401 Unauthorized without secret`() {
        // Arrange
        val webhookPayload = mapOf("eventType" to "ACTIVITY")

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/data-changed")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `handleApifyWebhook returns 401 Unauthorized with wrong secret`() {
        // Arrange
        val webhookPayload = mapOf("eventType" to "ACTIVITY")

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/data-changed")
                .header("X-Apify-Webhook-Secret", "wrong-secret")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `handleApifyWebhook handles TEST event`() {
        // Arrange
        val webhookPayload = mapOf("eventType" to "TEST")

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/data-changed")
                .header("X-Apify-Webhook-Secret", webhookSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isOk)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Test received")))

        // Verify no task was queued for TEST events
        verify(exactly = 0) { cloudTasksService.queueSyncTask(any()) }
    }

    @Test
    fun `handleApifyWebhook returns 400 Bad Request when datasetId is missing`() {
        // Arrange
        val webhookPayload = mapOf(
            "eventType" to "ACTIVITY",
            "resource" to mapOf("otherField" to "value")
        )

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/data-changed")
                .header("X-Apify-Webhook-Secret", webhookSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isBadRequest)
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Missing datasetId")))

        // Verify no task was queued
        verify(exactly = 0) { cloudTasksService.queueSyncTask(any()) }
    }

    @Test
    fun `handleApifyWebhook extracts datasetId from id field when defaultDatasetId is missing`() {
        // Arrange
        val webhookPayload = mapOf(
            "eventType" to "ACTIVITY",
            "resource" to mapOf("id" to "dataset-from-id-field")
        )

        every { cloudTasksService.queueSyncTask(any()) } returns "task-123"

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/data-changed")
                .header("X-Apify-Webhook-Secret", webhookSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isAccepted)

        // Verify datasetId was extracted from 'id' field
        val payloadSlot = slot<CloudTasksService.SyncTaskPayload>()
        verify { cloudTasksService.queueSyncTask(capture(payloadSlot)) }

        assertEquals("dataset-from-id-field", payloadSlot.captured.datasetId)
    }

    @Test
    fun `handleApifyWebhook prefers defaultDatasetId over id field`() {
        // Arrange
        val webhookPayload = mapOf(
            "eventType" to "ACTIVITY",
            "resource" to mapOf(
                "defaultDatasetId" to "preferred-dataset",
                "id" to "fallback-dataset"
            )
        )

        every { cloudTasksService.queueSyncTask(any()) } returns "task-123"

        // Act & Assert
        mockMvc.perform(
            post("/api/webhook/apify/data-changed")
                .header("X-Apify-Webhook-Secret", webhookSecret)
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(webhookPayload))
        )
            .andExpect(status().isAccepted)

        // Verify defaultDatasetId was used
        val payloadSlot = slot<CloudTasksService.SyncTaskPayload>()
        verify { cloudTasksService.queueSyncTask(capture(payloadSlot)) }

        assertEquals("preferred-dataset", payloadSlot.captured.datasetId)
    }
}
