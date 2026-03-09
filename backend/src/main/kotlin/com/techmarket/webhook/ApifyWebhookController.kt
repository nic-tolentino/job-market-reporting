package com.techmarket.webhook

import com.techmarket.config.ApifyProperties
import com.techmarket.sync.JobDataSyncService
import com.techmarket.util.Constants.WEBHOOK_EVENT_TEST
import com.techmarket.service.CloudTasksService
import com.techmarket.util.CloudTasksConstants
import com.techmarket.webhook.model.ApifyWebhookPayload
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/webhook/apify")
class ApifyWebhookController(
        private val cloudTasksService: CloudTasksService,
        private val apifyProperties: ApifyProperties
) {

    private val log = LoggerFactory.getLogger(ApifyWebhookController::class.java)

    @PostMapping("/data-changed")
    fun handleApifyWebhook(
            @RequestBody payload: ApifyWebhookPayload,
            @RequestHeader("X-Apify-Webhook-Secret", required = false) providedSecret: String?
    ): ResponseEntity<String> = handleWeightedWebhook(payload, providedSecret, null)

    @PostMapping("/nz/data-changed")
    fun handleApifyWebhookNz(
            @RequestBody payload: ApifyWebhookPayload,
            @RequestHeader("X-Apify-Webhook-Secret", required = false) providedSecret: String?
    ): ResponseEntity<String> = handleWeightedWebhook(payload, providedSecret, "NZ")

    @PostMapping("/au/data-changed")
    fun handleApifyWebhookAu(
            @RequestBody payload: ApifyWebhookPayload,
            @RequestHeader("X-Apify-Webhook-Secret", required = false) providedSecret: String?
    ): ResponseEntity<String> = handleWeightedWebhook(payload, providedSecret, "AU")

    private fun handleWeightedWebhook(
            payload: ApifyWebhookPayload,
            providedSecret: String?,
            countryCode: String?
    ): ResponseEntity<String> {
        val expectedSecret = apifyProperties.webhookSecret
        if (providedSecret != expectedSecret) {
            log.warn("Unauthorized webhook attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        log.info("Received Apify Data Change Notification (${countryCode ?: "Global"}): ${payload.eventType}")

        // Handle Test events from Apify UI
        if (payload.eventType == WEBHOOK_EVENT_TEST) {
            log.info("Received test event from Apify. Connectivity verified.")
            return ResponseEntity.ok("Test received")
        }

        // Extract dynamic dataset ID - look in both defaultDatasetId (Run resource) and id (Dataset resource)
        val resource = payload.resource
        val datasetId =
                (resource?.get("defaultDatasetId") as? String) ?: (resource?.get("id") as? String)

        if (datasetId == null) {
            log.error(
                    "Payload did not contain a dataset ID in 'defaultDatasetId' or 'id'. Resource keys: ${resource?.keys}"
            )
            return ResponseEntity.badRequest().body("Missing datasetId in payload")
        }

        log.info(
                "Extracted dynamic datasetId: $datasetId from webhook payload (${payload.eventType}) for country: ${countryCode ?: "Global"}"
        )

        // Generate correlation ID for tracing
        val correlationId = UUID.randomUUID().toString()

        // Queue task for background processing via Cloud Tasks
        val taskPayload = CloudTasksService.SyncTaskPayload(
            datasetId = datasetId,
            source = CloudTasksConstants.Source.APIFY,
            country = countryCode,
            triggeredBy = CloudTasksConstants.TriggeredBy.WEBHOOK,
            correlationId = correlationId
        )

        cloudTasksService.queueSyncTask(taskPayload)

        // Immediately return 202 Accepted so Apify doesn't timeout
        return ResponseEntity.accepted().body("Data sync queued for background processing (correlationId: $correlationId)")
    }
}
