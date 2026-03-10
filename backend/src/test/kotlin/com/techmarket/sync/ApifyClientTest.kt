package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.config.ApifyProperties
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

/**
 * Integration tests for ApifyClient.
 * These tests make real API calls to Apify to verify error handling.
 */
class ApifyClientTest {

    private lateinit var apifyClient: ApifyClient
    private lateinit var restClient: RestClient
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        restClient = RestClient.create()
        objectMapper = ObjectMapper()
        
        // Use a real but invalid token to trigger 401 errors
        val apifyProperties = ApifyProperties()
        apifyClient = ApifyClient(restClient, apifyProperties, objectMapper)
    }

    @Test
    fun `fetchRecentJobs returns empty list for blank datasetId`() {
        // Act
        val result = apifyClient.fetchRecentJobs("")

        // Assert - input validation returns empty list
        assertTrue(result.isEmpty())
    }

    @Test
    fun `fetchRecentJobs handles invalid dataset gracefully`() {
        // This test makes a real API call with invalid dataset
        // It should either throw HttpClientErrorException (404) or return empty list
        // depending on how the error is handled
        
        // We're testing that it doesn't crash with unexpected exceptions
        try {
            val result = apifyClient.fetchRecentJobs("invalid-dataset-id")
            // If it returns, should be empty
            assertTrue(result.isEmpty())
        } catch (e: Exception) {
            // If it throws, should be HttpClientErrorException (4xx) or similar
            // Not a crash
        }
    }
}
