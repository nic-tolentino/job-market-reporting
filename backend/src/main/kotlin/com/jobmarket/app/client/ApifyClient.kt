package com.jobmarket.app.client

import com.jobmarket.app.config.ApifyProperties
import com.jobmarket.app.dto.ApifyJobDto
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ApifyClient(
    private val apifyRestClient: RestClient,
    private val apifyProperties: ApifyProperties
) {
    private val log = LoggerFactory.getLogger(ApifyClient::class.java)

    fun fetchRecentJobs(): List<ApifyJobDto> {
        val datasetId = apifyProperties.datasetId
        
        if (datasetId.isBlank()) {
            log.warn("Apify datasetId is not configured. Returning empty list.")
            return emptyList()
        }

        log.info("Fetching jobs from Apify dataset: {}", datasetId)

        val uri = "/datasets/$datasetId/items?format=json&clean=true"
        
        return try {
            val response = apifyRestClient.get()
                .uri(uri)
                .retrieve()
                .body(object : ParameterizedTypeReference<List<ApifyJobDto>>() {})

            val jobs = response ?: emptyList()
            log.info("Successfully fetched {} jobs from Apify.", jobs.size)
            jobs
        } catch (e: Exception) {
            log.error("Failed to fetch jobs from Apify: {}", e.message, e)
            emptyList()
        }
    }
}
