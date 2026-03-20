package com.techmarket.sync.ats.ashby

import com.techmarket.config.AshbyProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient

@RestClientTest(AshbyClient::class)
class AshbyClientTest {

    @Autowired private lateinit var client: AshbyClient
    @Autowired private lateinit var server: MockRestServiceServer

    @TestConfiguration
    class TestConfig {
        @Bean fun ashbyProperties(): AshbyProperties = AshbyProperties()

        @Bean
        fun ashbyRestClient(
            builder: RestClient.Builder,
            properties: AshbyProperties
        ): RestClient = builder.baseUrl(properties.baseUrl).build()
    }

    @Test
    fun `should fetch jobs for a simple slug`() {
        val expectedResponse = """{"jobPostings":[]}"""

        server.expect(requestTo("https://api.ashbyhq.com/posting-api/job-board/acme"))
            .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON))

        val response = client.fetchJobs("acme")

        assertEquals(expectedResponse, response)
    }

    @Test
    fun `should encode slugs containing spaces`() {
        val expectedResponse = """{"jobPostings":[]}"""

        // Spring's UriBuilder percent-encodes spaces as %20
        server.expect(requestTo("https://api.ashbyhq.com/posting-api/job-board/Checkbox%20Technology"))
            .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON))

        val response = client.fetchJobs("Checkbox Technology")

        assertEquals(expectedResponse, response)
    }

    @Test
    fun `should throw on HTTP errors`() {
        server.expect(requestTo("https://api.ashbyhq.com/posting-api/job-board/bad-slug"))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

        assertThrows(Exception::class.java) { client.fetchJobs("bad-slug") }
    }
}
