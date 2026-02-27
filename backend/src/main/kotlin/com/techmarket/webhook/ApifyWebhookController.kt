package com.techmarket.webhook

import com.techmarket.config.ApifyProperties
import com.techmarket.sync.JobDataSyncService
import com.techmarket.webhook.model.ApifyWebhookPayload
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/webhook/apify")
class ApifyWebhookController(
        private val jobDataSyncService: JobDataSyncService,
        private val apifyProperties: ApifyProperties
) {

    private val log = LoggerFactory.getLogger(ApifyWebhookController::class.java)

    @PostMapping("/data-changed")
    fun handleApifyWebhook(
            @RequestBody payload: ApifyWebhookPayload,
            @RequestHeader("X-Apify-Webhook-Secret", required = false) providedSecret: String?
    ): ResponseEntity<String> {
        val expectedSecret = apifyProperties.webhookSecret
        if (providedSecret != expectedSecret) {
            log.warn("Unauthorized webhook attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        log.info("Received Apify Data Change Notification: ${payload.eventType}")

        // Handle Test events from Apify UI
        if (payload.eventType == "TEST") {
            log.info("Received test event from Apify. Connectivity verified.")
            return ResponseEntity.ok("Test received")
        }

        // Extract dynamic dataset ID - look in both defaultDatasetId (Run resource) and id (Dataset
        // resource)
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
                "Extracted dynamic datasetId: $datasetId from webhook payload (${payload.eventType})"
        )

        // Trigger the asynchronous ingestion process
        jobDataSyncService.runDataSync(datasetId)

        // Immediately return 202 Accepted so Apify doesn't timeout
        return ResponseEntity.accepted().body("Data sync triggered asynchronously")
    }
}
