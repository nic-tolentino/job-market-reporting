package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.config.ApifyProperties
import com.techmarket.sync.model.ApifyJobDto
import com.techmarket.sync.model.ApifyJobResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class ApifyClient(
        private val apifyRestClient: RestClient,
        private val apifyProperties: ApifyProperties,
        private val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(ApifyClient::class.java)

    fun fetchRecentJobs(datasetId: String): List<ApifyJobResult> {
        if (datasetId.isBlank()) {
            log.warn("Provided datasetId is empty. Returning empty list.")
            return emptyList()
        }

        log.info("Fetching jobs from Apify dataset: {}", datasetId)

        val uri = "/datasets/$datasetId/items?format=json&clean=true"

        return try {
            val responseStr = apifyRestClient.get().uri(uri).retrieve().body(String::class.java)

            if (responseStr.isNullOrBlank()) {
                log.info("Apify dataset is empty or returned null.")
                return emptyList()
            }

            val jsonArray = objectMapper.readTree(responseStr)
            val results = mutableListOf<ApifyJobResult>()

            if (jsonArray.isArray) {
                for (node in jsonArray) {
                    val dto = objectMapper.treeToValue(node, ApifyJobDto::class.java)
                    val rawJson = objectMapper.writeValueAsString(node)
                    results.add(ApifyJobResult(dto, rawJson))
                }
            }

            log.info("Successfully fetched {} jobs from Apify.", results.size)
            results
        } catch (e: Exception) {
            log.error("Failed to fetch jobs from Apify: {}", e.message, e)
            emptyList()
        }
    }
}
