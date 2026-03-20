package com.techmarket.sync.ats.lever

import com.techmarket.sync.ats.AtsClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Implementation of [AtsClient] for Lever. Uses the public Job Board API:
 * https://help.lever.co/hc/en-us/articles/206457365-Job-Board-Listing-API
 *
 * Returns a JSON array of job postings. The `mode=json` parameter ensures the
 * response is machine-readable JSON rather than HTML.
 */
@Component
class LeverClient(@Qualifier("leverRestClient") private val restClient: RestClient) : AtsClient {

    private val log = LoggerFactory.getLogger(LeverClient::class.java)

    override fun fetchJobs(identifier: String): String {
        log.info("Lever: Fetching all jobs for company: $identifier")

        return try {
            val response =
                restClient
                    .get()
                    .uri {
                        it.path("/postings/{slug}")
                            .queryParam("mode", "json")
                            .build(identifier)
                    }
                    .retrieve()
                    .body(String::class.java)

            if (response == null) {
                log.warn("Lever: Received empty response for company: $identifier")
                return "[]"
            }

            log.info("Lever: Successfully fetched job data for company: $identifier")
            response
        } catch (e: Exception) {
            log.error("Lever: Failed to fetch jobs for company $identifier: ${e.message}", e)
            throw e
        }
    }
}
