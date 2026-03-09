package com.techmarket.service

import com.techmarket.util.HealthCheckConstants
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Unit tests for HttpHealthChecker URL status classification logic.
 * 
 * Note: Full HTTP mocking requires WireMock or similar. These tests verify
 * the enum values and data structures are correct.
 */
class HttpHealthCheckerTest {

    @Test
    fun `UrlStatus enum has all expected values`() {
        // Verify all expected status values exist
        val statuses = HttpHealthChecker.UrlStatus.values()
        
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.ACTIVE })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.CLOSED_404 })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.CLOSED_410 })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.CLOSED_REDIRECT })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.CLOSED_NO_LONGER })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.CLOSED_FILLED })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.UNVERIFIED_LOGIN })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.UNVERIFIED_TIMEOUT })
        assertTrue(statuses.any { it.name == HealthCheckConstants.UrlStatus.UNVERIFIED_ERROR })
    }

    @Test
    fun `HealthCheckResult data class works correctly`() {
        // Arrange
        val result = HttpHealthChecker.HealthCheckResult(
            url = "https://example.com/job/123",
            status = HttpHealthChecker.UrlStatus.ACTIVE,
            httpStatusCode = HealthCheckConstants.HttpStatus.OK,
            redirectUrl = null,
            failureReason = null,
            responseTimeMs = 150
        )

        // Assert
        assertEquals("https://example.com/job/123", result.url)
        assertEquals(HttpHealthChecker.UrlStatus.ACTIVE, result.status)
        assertEquals(HealthCheckConstants.HttpStatus.OK, result.httpStatusCode)
        assertNull(result.redirectUrl)
        assertNull(result.failureReason)
        assertEquals(150, result.responseTimeMs)
    }

    @Test
    fun `HealthCheckResult captures closed job correctly`() {
        // Arrange
        val result = HttpHealthChecker.HealthCheckResult(
            url = "https://example.com/job/456",
            status = HttpHealthChecker.UrlStatus.CLOSED_404,
            httpStatusCode = HealthCheckConstants.HttpStatus.NOT_FOUND,
            redirectUrl = null,
            failureReason = "404 Not Found",
            responseTimeMs = 50
        )

        // Assert
        assertEquals(HttpHealthChecker.UrlStatus.CLOSED_404, result.status)
        assertEquals(HealthCheckConstants.HttpStatus.NOT_FOUND, result.httpStatusCode)
        assertEquals("404 Not Found", result.failureReason)
    }

    @Test
    fun `HealthCheckResult captures redirect correctly`() {
        // Arrange
        val result = HttpHealthChecker.HealthCheckResult(
            url = "https://example.com/job/789",
            status = HttpHealthChecker.UrlStatus.CLOSED_REDIRECT,
            httpStatusCode = HealthCheckConstants.HttpStatus.FOUND,
            redirectUrl = "https://company.com/careers",
            failureReason = "Redirects to generic careers page",
            responseTimeMs = 75
        )

        // Assert
        assertEquals(HttpHealthChecker.UrlStatus.CLOSED_REDIRECT, result.status)
        assertEquals(HealthCheckConstants.HttpStatus.FOUND, result.httpStatusCode)
        assertEquals("https://company.com/careers", result.redirectUrl)
        assertEquals("Redirects to generic careers page", result.failureReason)
    }

    @Test
    fun `HealthCheckResult captures timeout correctly`() {
        // Arrange
        val result = HttpHealthChecker.HealthCheckResult(
            url = "https://slow-server.com/job/123",
            status = HttpHealthChecker.UrlStatus.UNVERIFIED_TIMEOUT,
            httpStatusCode = null,
            redirectUrl = null,
            failureReason = "Timeout: Read timed out",
            responseTimeMs = 10000
        )

        // Assert
        assertEquals(HttpHealthChecker.UrlStatus.UNVERIFIED_TIMEOUT, result.status)
        assertNull(result.httpStatusCode)
        assertTrue(result.failureReason?.contains("Timeout") == true)
    }

    @Test
    fun `UrlStatus helper methods work correctly`() {
        // Test isClosed
        assertTrue(HealthCheckConstants.UrlStatus.isClosed(HealthCheckConstants.UrlStatus.CLOSED_404))
        assertTrue(HealthCheckConstants.UrlStatus.isClosed(HealthCheckConstants.UrlStatus.CLOSED_410))
        assertTrue(HealthCheckConstants.UrlStatus.isClosed(HealthCheckConstants.UrlStatus.CLOSED_REDIRECT))
        assertFalse(HealthCheckConstants.UrlStatus.isClosed(HealthCheckConstants.UrlStatus.ACTIVE))
        assertFalse(HealthCheckConstants.UrlStatus.isClosed(HealthCheckConstants.UrlStatus.UNKNOWN))
        assertFalse(HealthCheckConstants.UrlStatus.isClosed(null))

        // Test isUnverified
        assertTrue(HealthCheckConstants.UrlStatus.isUnverified(HealthCheckConstants.UrlStatus.UNVERIFIED_TIMEOUT))
        assertTrue(HealthCheckConstants.UrlStatus.isUnverified(HealthCheckConstants.UrlStatus.UNVERIFIED_LOGIN))
        assertTrue(HealthCheckConstants.UrlStatus.isUnverified(HealthCheckConstants.UrlStatus.UNVERIFIED_ERROR))
        assertFalse(HealthCheckConstants.UrlStatus.isUnverified(HealthCheckConstants.UrlStatus.ACTIVE))
        assertFalse(HealthCheckConstants.UrlStatus.isUnverified(null))
    }
}
