package com.jobmarket.app.controller

import com.jobmarket.app.dto.ApifyWebhookPayload
import com.jobmarket.app.service.JobDataSyncService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/webhook/apify")
class WebhookController(private val jobDataSyncService: JobDataSyncService) {

    private val log = LoggerFactory.getLogger(WebhookController::class.java)

    @PostMapping("/data-changed")
    fun handleApifyWebhook(@RequestBody payload: ApifyWebhookPayload): ResponseEntity<String> {
        log.info("Received Apify Data Change Notification: ${payload.eventType}")

        // Optionally log event details as requested by user
        if (log.isDebugEnabled) {
            log.debug(
                    "Webhook Payload Details: userId=${payload.userId}, createdAt=${payload.createdAt}"
            )
        }

        // Trigger the asynchronous ingestion process
        jobDataSyncService.runDataSync()

        // Immediately return 202 Accepted so Apify doesn't timeout
        return ResponseEntity.accepted().body("Data sync triggered asynchronously")
    }
}
