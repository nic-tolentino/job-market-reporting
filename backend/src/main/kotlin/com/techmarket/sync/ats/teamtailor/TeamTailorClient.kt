package com.techmarket.sync.ats.teamtailor

import com.techmarket.sync.ats.AtsClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Implementation of [AtsClient] for TeamTailor. Uses the public JSON feed:
 *   GET https://{identifier}.teamtailor.com/feed/jobs.json
 *
 * The identifier is the company subdomain (e.g. "autoguru", "eftsure").
 * TeamTailor boards have a unique subdomain per company — the RestClient has no fixed
 * base URL; the full URL is constructed per request.
 */
@Component
class TeamTailorClient(
    @Qualifier("teamTailorRestClient") private val restClient: RestClient
) : AtsClient {

    private val log = LoggerFactory.getLogger(TeamTailorClient::class.java)

    override fun fetchJobs(identifier: String): String {
        log.info("TeamTailor: Fetching all jobs for company: $identifier")

        return try {
            val response = restClient
                .get()
                .uri { builder ->
                    builder.scheme("https")
                        .host("$identifier.teamtailor.com")
                        .path("/feed/jobs.json")
                        .build()
                }
                .retrieve()
                .body(String::class.java)

            if (response == null) {
                log.warn("TeamTailor: Empty response for company: $identifier")
                return "[]"
            }

            log.info("TeamTailor: Successfully fetched job data for company: $identifier")
            response
        } catch (e: Exception) {
            log.error("TeamTailor: Failed to fetch jobs for company $identifier: ${e.message}", e)
            throw e
        }
    }
}
