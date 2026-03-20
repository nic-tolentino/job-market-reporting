package com.techmarket.sync.ats.workable

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techmarket.sync.ats.AtsClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Implementation of [AtsClient] for Workable. Uses the public jobs API:
 *   POST https://apply.workable.com/api/v3/accounts/{slug}/jobs
 *
 * The identifier is the company slug (e.g. "mathspace", "theta", "ct").
 *
 * Request body: `{"query":"","location":[],"department":[],"worktype":[],"remote":[]}`
 * For subsequent pages, add `"after": "<cursor>"` to the body.
 *
 * Response: `{"total": N, "results": [...], "paging": {"next": "<cursor>"}}`
 */
@Component
class WorkableClient(
    @Qualifier("workableRestClient") private val restClient: RestClient
) : AtsClient {

    private val log    = LoggerFactory.getLogger(WorkableClient::class.java)
    private val mapper = jacksonObjectMapper()

    override fun fetchJobs(identifier: String): String {
        log.info("Workable: Fetching all jobs for company: $identifier")

        val allJobs = mapper.createArrayNode()
        var cursor: String? = null
        var total = 0

        do {
            val requestBody = mapper.createObjectNode().apply {
                put("query", "")
                putArray("location")
                putArray("department")
                putArray("worktype")
                putArray("remote")
                if (cursor != null) put("after", cursor)
            }

            val raw = try {
                restClient.post()
                    .uri { it.path("/api/v3/accounts/{slug}/jobs").build(identifier) }
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(mapper.writeValueAsString(requestBody))
                    .retrieve()
                    .body(String::class.java)
            } catch (e: Exception) {
                log.error("Workable: Failed to fetch jobs for $identifier: ${e.message}", e)
                throw e
            }

            if (raw == null) {
                log.warn("Workable: Empty response for company: $identifier")
                break
            }

            val page = mapper.readTree(raw)
            if (cursor == null) total = page.path("total").asInt(0)

            val results = page.path("results")
            if (!results.isArray || results.size() == 0) break
            (results as ArrayNode).forEach { allJobs.add(it) }

            cursor = page.path("paging").path("next").asText(null)
                ?.takeIf { it.isNotBlank() }
        } while (cursor != null)

        val result = mapper.createObjectNode()
        result.put("total", total)
        result.set<ArrayNode>("results", allJobs)

        log.info("Workable: Fetched ${allJobs.size()} jobs for company: $identifier")
        return mapper.writeValueAsString(result)
    }
}
