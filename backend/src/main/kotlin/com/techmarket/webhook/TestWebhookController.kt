package com.techmarket.webhook

import com.techmarket.config.ApifyProperties
import com.techmarket.sync.JobDataSyncService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Controller strictly used for manual testing of the data synchronization process. We restrict this
 * to the "local" profile only, so it isn't exposed in production.
 */
@RestController
@RequestMapping("/api/sync")
@Profile("local")
class TestWebhookController(
        private val jobDataSyncService: JobDataSyncService,
        private val apifyProperties: ApifyProperties
) {

    @GetMapping("/test")
    fun testSync(@RequestParam(required = false) datasetId: String?): String {
        // Prefer the query parameter, fallback to application.yml
        val effectiveId = datasetId ?: apifyProperties.datasetId

        if (effectiveId.isBlank()) {
            return "Error: Provide a datasetId as a query param (?datasetId=...) or configure apify.dataset-id in application.yml"
        }

        jobDataSyncService.runDataSync(effectiveId)
        return "Manual Data Sync Pipeline executed for dataset: $effectiveId. Check local console logs for progress."
    }
}
