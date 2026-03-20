package com.techmarket.sync.ats.ashby

import com.techmarket.sync.ats.AtsClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Implementation of [AtsClient] for Ashby. Uses the public Job Board API:
 * https://developers.ashbyhq.com/reference/jobboardpublicapilistjobpostings
 *
 * The identifier (board slug) may contain spaces or mixed case
 * (e.g. "Checkbox Technology", "leonardo.ai") — Spring's RestClient handles
 * path-variable encoding automatically via UriBuilder.build().
 */
@Component
class AshbyClient(@Qualifier("ashbyRestClient") private val restClient: RestClient) : AtsClient {

    private val log = LoggerFactory.getLogger(AshbyClient::class.java)

    override fun fetchJobs(identifier: String): String {
        log.info("Ashby: Fetching all jobs for board: $identifier")

        return try {
            val response =
                restClient
                    .get()
                    .uri {
                        it.path("/posting-api/job-board/{slug}")
                            .build(identifier)
                    }
                    .retrieve()
                    .body(String::class.java)

            if (response == null) {
                log.warn("Ashby: Received empty response for board: $identifier")
                return "{}"
            }

            log.info("Ashby: Successfully fetched job data for board: $identifier")
            response
        } catch (e: Exception) {
            log.error("Ashby: Failed to fetch jobs for board $identifier: ${e.message}", e)
            throw e
        }
    }
}
