package com.techmarket.sync.ats.smartrecruiters

import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techmarket.sync.ats.AtsClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * Implementation of [AtsClient] for SmartRecruiters. Uses the public postings API:
 * https://dev.smartrecruiters.com/customer-api/live-jobs-api/
 *
 * Endpoint: GET /v1/companies/{slug}/postings?limit=100&offset={offset}
 *
 * The slug is case-sensitive and must match the canonical form returned by the redirect check
 * (jobs.smartrecruiters.com/{slug} → careers.smartrecruiters.com/{slug}).
 *
 * Returns a paginated response: `{"totalFound": N, "content": [...]}`. All pages are fetched
 * and merged into a single JSON object before returning.
 */
@Component
class SmartRecruitersClient(
    @Qualifier("smartRecruitersRestClient") private val restClient: RestClient
) : AtsClient {

    private val log    = LoggerFactory.getLogger(SmartRecruitersClient::class.java)
    private val mapper = jacksonObjectMapper()

    companion object {
        private const val PAGE_SIZE = 100
    }

    override fun fetchJobs(identifier: String): String {
        log.info("SmartRecruiters: Fetching all jobs for company: $identifier")

        val allJobs = mapper.createArrayNode()
        var offset  = 0
        var total   = Int.MAX_VALUE

        while (offset < total) {
            val currentOffset = offset
            val raw = try {
                restClient.get()
                    .uri {
                        it.path("/v1/companies/{slug}/postings")
                            .queryParam("limit", PAGE_SIZE)
                            .queryParam("offset", currentOffset)
                            .build(identifier)
                    }
                    .retrieve()
                    .body(String::class.java)
            } catch (e: Exception) {
                log.error("SmartRecruiters: Failed to fetch jobs for $identifier at offset $currentOffset: ${e.message}", e)
                throw e
            }

            if (raw == null) {
                log.warn("SmartRecruiters: Empty response for $identifier at offset $currentOffset")
                break
            }

            val page = mapper.readTree(raw)
            if (offset == 0) {
                total = page.path("totalFound").asInt(0)
            }

            val content = page.path("content")
            if (!content.isArray || content.size() == 0) break
            (content as ArrayNode).forEach { allJobs.add(it) }
            offset += content.size()

            if (content.size() < PAGE_SIZE) break
        }

        val result = mapper.createObjectNode()
        result.put("totalFound", total.coerceAtMost(allJobs.size()))
        result.set<ArrayNode>("content", allJobs)

        log.info("SmartRecruiters: Fetched ${allJobs.size()} jobs for company: $identifier")
        return mapper.writeValueAsString(result)
    }
}
