package com.techmarket.api.internal

import com.techmarket.service.CloudTasksService
import com.techmarket.sync.JobDataSyncService
import com.techmarket.util.CloudTasksConstants
import com.techmarket.util.HealthCheckConstants.UrlStatus.isClosed
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Internal endpoint for processing sync tasks from Cloud Tasks.
 * Protected by OIDC token validation - only Cloud Tasks can call this endpoint.
 */
@RestController
@RequestMapping("/api/internal")
class SyncTaskHandler(
    private val jobDataSyncService: JobDataSyncService
) {

    private val log = LoggerFactory.getLogger(SyncTaskHandler::class.java)

    /**
     * Internal endpoint for processing sync tasks from Cloud Tasks.
     * 
     * Security: Cloud Tasks attaches an OIDC token to the Authorization header.
     * The token is validated by Google Cloud Run before the request reaches this handler.
     * 
     * @param taskPayload The sync task payload
     * @param authorization Bearer token from Cloud Tasks (OIDC token)
     * @param taskRetryCount The number of retry attempts (0 for first attempt)
     * @param taskName The full task name for tracking
     */
    @PostMapping("/process-sync")
    fun processSync(
        @RequestBody taskPayload: CloudTasksService.SyncTaskPayload,
        @RequestHeader("Authorization") authorization: String?,
        @RequestHeader("X-CloudTasks-TaskRetryCount", required = false) taskRetryCount: Int?,
        @RequestHeader("X-Cloud-Tasks-Task-Name") taskName: String
    ): ResponseEntity<Unit> {
        // Validate OIDC token is present (Cloud Run validates the token signature)
        // We just need to ensure the header exists
        if (authorization.isNullOrBlank() || !authorization.startsWith("Bearer ")) {
            log.warn("Invalid or missing Authorization header - request may not be from Cloud Tasks")
            throw AccessDeniedException("Invalid Cloud Tasks request - missing OIDC token")
        }

        val retryCount = taskRetryCount ?: 0
        val correlationId = taskPayload.correlationId
        log.info("Processing sync task, correlationId=$correlationId, datasetId=${taskPayload.datasetId}, attempt=${retryCount + 1}")

        try {
            // Execute sync logic based on source type
            when (taskPayload.source) {
                CloudTasksConstants.Source.APIFY -> {
                    jobDataSyncService.runDataSync(taskPayload.datasetId, taskPayload.country)
                }
                CloudTasksConstants.Source.MANUAL, CloudTasksConstants.Source.SCHEDULED -> {
                    jobDataSyncService.runDataSync(taskPayload.datasetId, taskPayload.country)
                }
                else -> {
                    log.warn("Unknown source type: ${taskPayload.source}, correlationId=$correlationId")
                    // Permanent error - don't retry
                    log.info("Task marked as failed (permanent error), correlationId=$correlationId")
                    return ResponseEntity.ok().build()
                }
            }

            log.info("Sync completed successfully, correlationId=$correlationId")
            return ResponseEntity.ok().build()

        } catch (e: Exception) {
            log.error("Sync failed, correlationId=$correlationId, attempt=${retryCount + 1}: ${e.message}", e)

            // Check if this is a transient or permanent error
            val isTransientError = isTransientError(e)

            if (!isTransientError) {
                // Permanent error - don't retry, just log and return success to remove from queue
                log.error("Permanent error detected, task will not be retried, correlationId=$correlationId")
                return ResponseEntity.ok().build()
            }

            // Transient error - check if we should stop retrying
            if (retryCount >= 4) {
                log.error("Task moving to DLQ after ${retryCount + 1} attempts, correlationId=$correlationId")
                // Optionally: Send alert to Slack/email here
            }

            // Re-throw to trigger Cloud Tasks retry
            throw e
        }
    }

    /**
     * Determines if an error is transient (should retry) or permanent (should not retry).
     */
    private fun isTransientError(exception: Exception): Boolean {
        val message = exception.message?.lowercase() ?: return true

        // Transient errors - worth retrying
        val transientPatterns = listOf(
            "timeout",
            "connection",
            "network",
            "unavailable",
            "rate limit",
            "throttl",
            "temporarily",
            "try again"
        )

        // Permanent errors - should not retry
        val permanentPatterns = listOf(
            "invalid",
            "not found",
            "malformed",
            "parse",
            "authentication",
            "authorization",
            "forbidden",
            "permanent"
        )

        // Check for permanent errors first
        if (permanentPatterns.any { message.contains(it) }) {
            return false
        }

        // Check for transient errors
        return transientPatterns.any { message.contains(it) }
    }
}
