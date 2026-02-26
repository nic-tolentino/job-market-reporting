package com.jobmarket.app.webhook

import com.jobmarket.app.sync.JobDataSyncService
import com.jobmarket.app.webhook.model.ApifyWebhookPayload
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/webhook/apify")
class ApifyWebhookController(private val jobDataSyncService: JobDataSyncService) {

    private val log = LoggerFactory.getLogger(ApifyWebhookController::class.java)

    @PostMapping("/data-changed")
    fun handleApifyWebhook(
            @RequestBody payload: ApifyWebhookPayload,
            @RequestHeader("X-Apify-Webhook-Secret", required = false) providedSecret: String?,
            @Value("\${apify.webhook-secret:default-local-secret}") expectedSecret: String
    ): ResponseEntity<String> {
        if (providedSecret != expectedSecret) {
            log.warn("Unauthorized webhook attempt. Invalid or missing secret.")
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized")
        }

        log.info("Received Apify Data Change Notification: \${payload.eventType}")

        // Optionally log event details as requested by user
        if (log.isDebugEnabled) {
            log.debug(
                    "Webhook Payload Details: userId=${payload.userId}, createdAt=${payload.createdAt}"
            )
        }

        // Extract dynamic dataset ID from Apify payload if present
        val datasetId = payload.resource?.get("defaultDatasetId") as? String
        if (datasetId == null) {
            log.error("Payload did not contain a defaultDatasetId in the resource object.")
            return ResponseEntity.badRequest().body("Missing defaultDatasetId in payload")
        }

        log.info("Extracted dynamic datasetId: $datasetId from webhook payload")

        // Trigger the asynchronous ingestion process
        jobDataSyncService.runDataSync(datasetId)

        // Immediately return 202 Accepted so Apify doesn't timeout
        return ResponseEntity.accepted().body("Data sync triggered asynchronously")
    }
}
