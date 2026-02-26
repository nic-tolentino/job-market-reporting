package com.jobmarket.app.webhook

import com.jobmarket.app.config.ApifyProperties
import com.jobmarket.app.sync.JobDataSyncService
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
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
    fun testSync(): String {
        val datasetId = apifyProperties.datasetId
        if (datasetId.isBlank()) {
            return "Error: apify.dataset-id is not configured in application.yml"
        }
        jobDataSyncService.runDataSync(datasetId)
        return "Data Sync Pipeline executed for dataset $datasetId. Check local console logs for mapped output."
    }
}
