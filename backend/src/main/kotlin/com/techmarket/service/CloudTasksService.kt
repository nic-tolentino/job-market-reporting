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

        // Configure OIDC token for secure authentication
        // Cloud Tasks will obtain and attach an OIDC token signed by Google
        val oidcToken = OidcToken.newBuilder()
            .setServiceAccountEmail("$projectId-compute@developer.gserviceaccount.com")
            .build()

        val task = Task.newBuilder()
            .setHttpRequest(
                HttpRequest.newBuilder()
                    .setHttpMethod(HttpMethod.POST)
                    .setUrl(handlerUrl)
                    .setBody(
                        ByteString.copyFrom(
                            payload.toJson().toByteArray(StandardCharsets.UTF_8)
                        )
                    )
                    .putHeaders("Content-Type", "application/json")
                    .setOidcToken(oidcToken)
                    .build()
            )
            .build()

        val request = CreateTaskRequest.newBuilder()
            .setParent(queuePath)
            .setTask(task)
            .build()

        val createdTask = cloudTasksClient.createTask(request)
        log.info("Queued Cloud Task: ${createdTask.name} for dataset ${payload.datasetId}")
        return createdTask.name
    }

    /**
     * Payload for sync tasks.
     */
    data class SyncTaskPayload(
        val datasetId: String,        // The dataset ID to process
        val source: String,           // APIFY, ATS, MANUAL
        val country: String?,
        val triggeredBy: String,      // WEBHOOK, SCHEDULED, ADMIN
        val correlationId: String     // For tracing/debugging
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
