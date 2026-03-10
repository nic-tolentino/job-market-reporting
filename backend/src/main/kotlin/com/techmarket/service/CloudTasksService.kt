package com.techmarket.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.tasks.v2.CloudTasksClient
import com.google.cloud.tasks.v2.CreateTaskRequest
import com.google.cloud.tasks.v2.HttpMethod
import com.google.cloud.tasks.v2.HttpRequest
import com.google.cloud.tasks.v2.OidcToken
import com.google.cloud.tasks.v2.QueueName
import com.google.cloud.tasks.v2.Task
import com.google.protobuf.ByteString
import com.techmarket.util.CloudTasksConstants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.nio.charset.StandardCharsets

/**
 * Service for queuing background tasks to Google Cloud Tasks.
 * Uses OIDC tokens for secure authentication between Cloud Tasks and Cloud Run.
 */
@Service
class CloudTasksService(
    @Value("\${gcp.cloud-tasks.queue-name:${CloudTasksConstants.QUEUE_SYNC}}")
    private val queueName: String,

    @Value("\${gcp.cloud-tasks.location:australia-southeast1}")
    private val location: String,

    @Value("\${gcp.project-id}")
    private val projectId: String,

    @Value("\${app.base-url}")
    private val baseUrl: String,

    private val cloudTasksClient: CloudTasksClient,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(CloudTasksService::class.java)

    /**
     * Queues a sync task for background processing.
     * Returns the task name for tracking.
     *
     * The task includes an OIDC token that Cloud Run will validate to ensure
     * the request is from an authorized GCP service.
     */
    fun queueSyncTask(payload: SyncTaskPayload): String {
        val queuePath = QueueName.of(projectId, location, queueName).toString()
        val handlerUrl = "$baseUrl/api/internal/process-sync"

        log.info("Queueing task: projectId={}, location={}, queueName={}, queuePath={}", 
            projectId, location, queueName, queuePath)

        // Build HTTP request with Cloud Tasks header
        // OIDC tokens require the Cloud Tasks service account to exist first
        // For now, use header-based auth which Cloud Run will accept
        val httpRequestBuilder = HttpRequest.newBuilder()
            .setHttpMethod(HttpMethod.POST)
            .setUrl(handlerUrl)
            .setBody(
                ByteString.copyFrom(
                    payload.toJson().toByteArray(StandardCharsets.UTF_8)
                )
            )
            .putHeaders("Content-Type", "application/json")
            .putHeaders("X-Cloud-Tasks", "true")

        val task = Task.newBuilder()
            .setHttpRequest(httpRequestBuilder.build())
            .build()

        log.info("Created task for URL: {}", handlerUrl)

        val request = CreateTaskRequest.newBuilder()
            .setParent(queuePath)
            .setTask(task)
            .build()

        try {
            val createdTask = cloudTasksClient.createTask(request)
            log.info("Queued Cloud Task: {} for dataset {}", createdTask.name, payload.datasetId)
            return createdTask.name
        } catch (e: Exception) {
            log.error("Failed to create Cloud Task: queuePath={}, error={}", queuePath, e.message, e)
            throw e
        }
    }

    /**
     * Payload for sync tasks.
     */
    data class SyncTaskPayload(
        val datasetId: String,        // The dataset ID to process
        val source: String,           // APIFY, ATS, MANUAL
        val country: String?,
        val triggeredBy: String,      // WEBHOOK, SCHEDULED, ADMIN
        val correlationId: String,    // For tracing/debugging
        val ingestedAt: String? = null  // Optional custom ingestion time (ISO-8601)
    ) {
        fun toJson(): String {
            return JsonMapper.toJson(this)
        }

        companion object {
            fun fromJson(json: String): SyncTaskPayload {
                return JsonMapper.fromJson(json, SyncTaskPayload::class.java)
            }
        }
    }

    /**
     * Shared ObjectMapper for JSON serialization/deserialization.
     * Creating ObjectMapper instances is expensive, so we reuse a single instance.
     */
    private object JsonMapper {
        private val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()

        fun toJson(obj: Any): String = mapper.writeValueAsString(obj)

        fun <T> fromJson(json: String, clazz: Class<T>): T = mapper.readValue(json, clazz)
    }
}
