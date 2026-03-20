package com.techmarket.sync.ats.lever

import com.techmarket.config.LeverProperties
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

@RestClientTest(LeverClient::class)
class LeverClientTest {

    @Autowired private lateinit var client: LeverClient
    @Autowired private lateinit var server: MockRestServiceServer

    @TestConfiguration
    class TestConfig {
        @Bean fun leverProperties(): LeverProperties = LeverProperties()

        @Bean
        fun leverRestClient(
            builder: RestClient.Builder,
            properties: LeverProperties
        ): RestClient = builder.baseUrl(properties.baseUrl).build()
    }

    @Test
    fun `should fetch jobs successfully`() {
        val slug = "acme-corp"
        val expectedResponse = """[{"id":"abc","text":"Engineer"}]"""

        server.expect(requestTo("https://api.lever.co/v0/postings/acme-corp?mode=json"))
            .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON))

        val response = client.fetchJobs(slug)

        assertEquals(expectedResponse, response)
    }

    @Test
    fun `should throw on HTTP errors`() {
        server.expect(requestTo("https://api.lever.co/v0/postings/bad-slug?mode=json"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND))

        assertThrows(Exception::class.java) { client.fetchJobs("bad-slug") }
    }
}
