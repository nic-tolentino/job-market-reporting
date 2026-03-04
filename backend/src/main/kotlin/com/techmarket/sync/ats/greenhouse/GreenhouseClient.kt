package com.techmarket.sync.ats.greenhouse

import com.techmarket.sync.ats.AtsClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Implementation of [AtsClient] for Greenhouse. Uses the public Job Board API:
 * https://developers.greenhouse.io/job-board.html
 */
@Component
class GreenhouseClient(@Qualifier("greenhouseRestClient") private val restClient: RestClient) :
        AtsClient {

    private val log = LoggerFactory.getLogger(GreenhouseClient::class.java)

    override fun fetchJobs(identifier: String): String {
        log.info("Greenhouse: Fetching all jobs for board token: $identifier")

        return try {
            val response =
                    restClient
                            .get()
                            .uri {
                                it.path("/boards/{boardToken}/jobs")
                                        .queryParam("content", "true")
                                        .build(identifier)
                            }
                            .retrieve()
                            .body(String::class.java)

            if (response == null) {
                log.warn("Greenhouse: Received empty response for board: $identifier")
                return "{}"
            }

            log.info("Greenhouse: Successfully fetched job data for board: $identifier")
            response
        } catch (e: Exception) {
            log.error("Greenhouse: Failed to fetch jobs for board $identifier: ${e.message}", e)
            throw e
        }
    }
}
