package com.techmarket.sync.ats.greenhouse

import com.techmarket.config.GreenhouseProperties
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

@RestClientTest(GreenhouseClient::class)
class GreenhouseClientTest {

        @Autowired private lateinit var client: GreenhouseClient

        @Autowired private lateinit var server: MockRestServiceServer

        @TestConfiguration
        class TestConfig {
                @Bean fun greenhouseProperties(): GreenhouseProperties = GreenhouseProperties()

                @Bean
                fun greenhouseRestClient(
                        builder: RestClient.Builder,
                        properties: GreenhouseProperties
                ): RestClient {
                        return builder.baseUrl(properties.baseUrl).build()
                }
        }

        @Test
        fun `should fetch jobs successfully`() {
                val boardToken = "test-board"
                val expectedResponse = """{"jobs": []}"""

                server.expect(
                                requestTo(
                                        "https://boards-api.greenhouse.io/v1/boards/test-board/jobs?content=true"
                                )
                        )
                        .andRespond(withSuccess(expectedResponse, MediaType.APPLICATION_JSON))

                val response = client.fetchJobs(boardToken)

                assertEquals(expectedResponse, response)
        }

        @Test
        fun `should throw on HTTP errors`() {
                val boardToken = "test-board"

                server.expect(
                                requestTo(
                                        "https://boards-api.greenhouse.io/v1/boards/test-board/jobs?content=true"
                                )
                        )
                        .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR))

                assertThrows(Exception::class.java) { client.fetchJobs(boardToken) }
        }
}
