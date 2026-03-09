package com.techmarket.api.internal

import com.google.cloud.tasks.v2.CloudTasksClient
import com.google.cloud.tasks.v2.GetTaskRequest
import com.google.cloud.tasks.v2.Task
import com.techmarket.service.CloudTasksService
import com.techmarket.sync.JobDataSyncService
import com.techmarket.util.CloudTasksConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException

class SyncTaskHandlerTest {

    private lateinit var jobDataSyncService: JobDataSyncService
    private lateinit var cloudTasksClient: CloudTasksClient
    private lateinit var syncTaskHandler: SyncTaskHandler

    @BeforeEach
    fun setup() {
        jobDataSyncService = mockk<JobDataSyncService>(relaxed = true)

        syncTaskHandler = SyncTaskHandler(
            jobDataSyncService = jobDataSyncService
        )
    }

    @Test
    fun `processSync accepts valid Cloud Tasks request with APIFY source`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "dataset-123",
            source = CloudTasksConstants.Source.APIFY,
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "test-correlation"
        )

        // Act
        val response = syncTaskHandler.processSync(
            taskPayload = taskPayload,
            authorization = "Bearer valid-oidc-token",
            taskRetryCount = 0,
            taskName = "projects/test-project/locations/australia-southeast1/queues/tech-market-sync-queue/tasks/task-123"
        )

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        verify { jobDataSyncService.runDataSync("dataset-123", "NZ") }
    }

    @Test
    fun `processSync accepts valid Cloud Tasks request with MANUAL source`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "manual-dataset",
            source = CloudTasksConstants.Source.MANUAL,
            country = null,
            triggeredBy = CloudTasksConstants.TriggeredBy.ADMIN,
            correlationId = "admin-correlation"
        )

        // Act
        val response = syncTaskHandler.processSync(
            taskPayload = taskPayload,
            authorization = "Bearer valid-oidc-token",
            taskRetryCount = 0,
            taskName = "projects/test-project/locations/australia-southeast1/queues/tech-market-sync-queue/tasks/task-456"
        )

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        verify { jobDataSyncService.runDataSync("manual-dataset", null) }
    }

    @Test
    fun `processSync accepts valid Cloud Tasks request with SCHEDULED source`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "scheduled-dataset",
            source = CloudTasksConstants.Source.SCHEDULED,
            country = "AU",
            triggeredBy = CloudTasksConstants.TriggeredBy.SCHEDULED,
            correlationId = "scheduled-correlation"
        )

        // Act
        val response = syncTaskHandler.processSync(
            taskPayload = taskPayload,
            authorization = "Bearer valid-oidc-token",
            taskRetryCount = 0,
            taskName = "projects/test-project/locations/australia-southeast1/queues/tech-market-sync-queue/tasks/task-789"
        )

        // Assert
        assertEquals(HttpStatus.OK, response.statusCode)
        verify { jobDataSyncService.runDataSync("scheduled-dataset", "AU") }
    }

    @Test
    fun `processSync rejects request without Authorization header`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "test-dataset",
            source = CloudTasksConstants.Source.APIFY,
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "test-correlation"
        )

        // Act & Assert
        assertThrows<AccessDeniedException> {
            syncTaskHandler.processSync(
                taskPayload = taskPayload,
                authorization = null,
                taskRetryCount = 0,
                taskName = "task-123"
            )
        }
    }

    @Test
    fun `processSync rejects request with invalid Authorization header`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "test-dataset",
            source = CloudTasksConstants.Source.APIFY,
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "test-correlation"
        )

        // Act & Assert
        assertThrows<AccessDeniedException> {
            syncTaskHandler.processSync(
                taskPayload = taskPayload,
                authorization = "InvalidHeader",
                taskRetryCount = 0,
                taskName = "task-123"
            )
        }
    }

    @Test
    fun `processSync rethrows transient exception on sync failure`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "failing-dataset",
            source = CloudTasksConstants.Source.APIFY,
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "failing-correlation"
        )

        val syncException = RuntimeException("BigQuery timeout")
        every { jobDataSyncService.runDataSync("failing-dataset", "NZ") } throws syncException

        // Act & Assert
        val exception = assertThrows<RuntimeException> {
            syncTaskHandler.processSync(
                taskPayload = taskPayload,
                authorization = "Bearer valid-oidc-token",
                taskRetryCount = 2,
                taskName = "task-123"
            )
        }
        assertEquals("BigQuery timeout", exception.message)
    }

    @Test
    fun `processSync does not retry permanent errors`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "invalid-dataset",
            source = CloudTasksConstants.Source.APIFY,
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "invalid-correlation"
        )

        val syncException = IllegalArgumentException("Invalid dataset ID")
        every { jobDataSyncService.runDataSync("invalid-dataset", "NZ") } throws syncException

        // Act
        val response = syncTaskHandler.processSync(
            taskPayload = taskPayload,
            authorization = "Bearer valid-oidc-token",
            taskRetryCount = 0,
            taskName = "task-123"
        )

        // Assert - should return 200 OK (not retry)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `processSync handles unknown source type without retry`() {
        // Arrange
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = "test-dataset",
            source = "UNKNOWN_SOURCE",
            country = "NZ",
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = "test-correlation"
        )

        // Act
        val response = syncTaskHandler.processSync(
            taskPayload = taskPayload,
            authorization = "Bearer valid-oidc-token",
            taskRetryCount = 0,
            taskName = "task-123"
        )

        // Assert - should return 200 OK (permanent error, no retry)
        assertEquals(HttpStatus.OK, response.statusCode)
    }
}
