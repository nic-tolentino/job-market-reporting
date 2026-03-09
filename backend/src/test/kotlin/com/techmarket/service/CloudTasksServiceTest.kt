package com.techmarket.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.tasks.v2.CloudTasksClient
import com.google.cloud.tasks.v2.CreateTaskRequest
import com.google.cloud.tasks.v2.HttpMethod
import com.google.cloud.tasks.v2.HttpRequest
import com.google.cloud.tasks.v2.QueueName
import com.google.cloud.tasks.v2.Task
import com.techmarket.util.CloudTasksConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CloudTasksServiceTest {

    private lateinit var cloudTasksClient: CloudTasksClient
    private lateinit var cloudTasksService: CloudTasksService

    private val queueName = CloudTasksConstants.QUEUE_SYNC
    private val location = "australia-southeast1"
    private val projectId = "test-project"
    private val baseUrl = "https://test-backend.a.run.app"

    @BeforeEach
    fun setup() {
        cloudTasksClient = mockk<CloudTasksClient>(relaxed = true)
        val objectMapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

        // Mock a created task response
        val createdTask = mockk<Task>(relaxed = true)
        every { createdTask.name } returns "projects/$projectId/locations/$location/queues/$queueName/tasks/test-task-123"

        every {
            cloudTasksClient.createTask(any<CreateTaskRequest>())
        } returns createdTask

        cloudTasksService = CloudTasksService(
            queueName = queueName,
            location = location,
            projectId = projectId,
            baseUrl = baseUrl,
            cloudTasksClient = cloudTasksClient,
            objectMapper = objectMapper
        )
    }

    @Test
    fun `queueSyncTask creates task successfully`() {
        // Arrange
        val payload = CloudTasksService.SyncTaskPayload(
            datasetId = "test-dataset-123",
            source = CloudTasksConstants.Source.APIFY,
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "test-correlation-123"
        )

        // Act
        val taskName = cloudTasksService.queueSyncTask(payload)

        // Assert
        assertNotNull(taskName)
        assertTrue(taskName.contains(queueName))
        assertTrue(taskName.contains("tasks/"))

        // Verify the task was created with correct parameters
        val requestSlot = slot<CreateTaskRequest>()
        verify { cloudTasksClient.createTask(capture(requestSlot)) }

        val request = requestSlot.captured
        val task = request.task

        // Verify queue path
        assertTrue(request.parent.contains(queueName))

        // Verify HTTP request configuration
        val httpRequest = task.httpRequest
        assertEquals(HttpMethod.POST, httpRequest.httpMethod)
        assertEquals("$baseUrl/api/internal/process-sync", httpRequest.url)
        assertEquals("application/json", httpRequest.headersMap["Content-Type"])
        assertEquals("true", httpRequest.headersMap["X-Cloud-Tasks"])

        // Verify payload
        val bodyString = httpRequest.body.toStringUtf8()
        assertTrue(bodyString.contains("test-dataset-123"))
        assertTrue(bodyString.contains(CloudTasksConstants.Source.APIFY))
        assertTrue(bodyString.contains("NZ"))
        assertTrue(bodyString.contains("test-correlation-123"))
    }

    @Test
    fun `queueSyncTask creates task with MANUAL source`() {
        // Arrange
        val payload = CloudTasksService.SyncTaskPayload(
            datasetId = "manual-dataset",
            source = CloudTasksConstants.Source.MANUAL,
            country = null,
            triggeredBy = CloudTasksConstants.TriggeredBy.ADMIN,
            correlationId = "admin-correlation"
        )

        // Act
        cloudTasksService.queueSyncTask(payload)

        // Assert
        val requestSlot = slot<CreateTaskRequest>()
        verify { cloudTasksClient.createTask(capture(requestSlot)) }

        val bodyString = requestSlot.captured.task.httpRequest.body.toStringUtf8()
        assertTrue(bodyString.contains(CloudTasksConstants.Source.MANUAL))
        assertTrue(bodyString.contains(CloudTasksConstants.TriggeredBy.ADMIN))
    }

    @Test
    fun `queueSyncTask creates task with SCHEDULED source`() {
        // Arrange
        val payload = CloudTasksService.SyncTaskPayload(
            datasetId = "scheduled-dataset",
            source = CloudTasksConstants.Source.SCHEDULED,
            country = "AU",
            triggeredBy = CloudTasksConstants.TriggeredBy.SCHEDULED,
            correlationId = "scheduled-correlation"
        )

        // Act
        cloudTasksService.queueSyncTask(payload)

        // Assert
        val requestSlot = slot<CreateTaskRequest>()
        verify { cloudTasksClient.createTask(capture(requestSlot)) }

        val bodyString = requestSlot.captured.task.httpRequest.body.toStringUtf8()
        assertTrue(bodyString.contains(CloudTasksConstants.Source.SCHEDULED))
        assertTrue(bodyString.contains("AU"))
    }

    @Test
    fun `SyncTaskPayload toJson serializes correctly`() {
        // Arrange
        val payload = CloudTasksService.SyncTaskPayload(
            datasetId = "test-manifest",
            source = CloudTasksConstants.Source.APIFY,
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "test-123"
        )

        // Act
        val json = payload.toJson()

        // Assert
        assertNotNull(json)
        assertTrue(json.contains("\"datasetId\":\"test-manifest\""))
        assertTrue(json.contains("\"source\":\"${CloudTasksConstants.Source.APIFY}\""))
        assertTrue(json.contains("\"country\":\"NZ\""))
        assertTrue(json.contains("\"triggeredBy\":\"${CloudTasksConstants.TriggeredBy.WEBHOOK}\""))
        assertTrue(json.contains("\"correlationId\":\"test-123\""))
    }

    @Test
    fun `SyncTaskPayload fromJson deserializes correctly`() {
        // Arrange
        val json = """
            {
                "datasetId": "test-manifest",
                "source": "${CloudTasksConstants.Source.APIFY}",
                "country": "NZ",
                "triggeredBy": "${CloudTasksConstants.TriggeredBy.WEBHOOK}",
                "correlationId": "test-123"
            }
        """.trimIndent()

        // Act
        val payload = CloudTasksService.SyncTaskPayload.fromJson(json)

        // Assert
        assertEquals("test-manifest", payload.datasetId)
        assertEquals(CloudTasksConstants.Source.APIFY, payload.source)
        assertEquals("NZ", payload.country)
        assertEquals(CloudTasksConstants.TriggeredBy.WEBHOOK, payload.triggeredBy)
        assertEquals("test-123", payload.correlationId)
    }

    @Test
    fun `SyncTaskPayload fromJson handles null country`() {
        // Arrange
        val json = """
            {
                "datasetId": "test-manifest",
                "source": "${CloudTasksConstants.Source.MANUAL}",
                "country": null,
                "triggeredBy": "${CloudTasksConstants.TriggeredBy.ADMIN}",
                "correlationId": "test-123"
            }
        """.trimIndent()

        // Act
        val payload = CloudTasksService.SyncTaskPayload.fromJson(json)

        // Assert
        assertNull(payload.country)
        assertEquals(CloudTasksConstants.Source.MANUAL, payload.source)
    }

    @Test
    fun `SyncTaskPayload toJson and fromJson are symmetric`() {
        // Arrange
        val originalPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "gs://bucket/dataset/manifest.json",
            source = CloudTasksConstants.Source.ATS,
            country = "AU",
            triggeredBy = CloudTasksConstants.TriggeredBy.SCHEDULED,
            correlationId = "unique-correlation-456"
        )

        // Act
        val json = originalPayload.toJson()
        val deserializedPayload = CloudTasksService.SyncTaskPayload.fromJson(json)

        // Assert
        assertEquals(originalPayload.datasetId, deserializedPayload.datasetId)
        assertEquals(originalPayload.source, deserializedPayload.source)
        assertEquals(originalPayload.country, deserializedPayload.country)
        assertEquals(originalPayload.triggeredBy, deserializedPayload.triggeredBy)
        assertEquals(originalPayload.correlationId, deserializedPayload.correlationId)
    }
}
