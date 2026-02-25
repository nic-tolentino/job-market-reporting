package com.jobmarket.app.controller

import com.jobmarket.app.client.ApifyClient
import com.jobmarket.app.dto.ApifyJobDto
import org.springframework.context.annotation.Profile
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller strictly used for manual testing of the ingestion process.
 * We restrict this to the "local" profile only, so it isn't exposed in production.
 */
@RestController
@RequestMapping("/api/ingest")
@Profile("local")
class IngestionController(
    private val apifyClient: ApifyClient
) {

    @GetMapping("/test")
    fun testIngestion(): List<ApifyJobDto> {
        return apifyClient.fetchRecentJobs()
    }
}
