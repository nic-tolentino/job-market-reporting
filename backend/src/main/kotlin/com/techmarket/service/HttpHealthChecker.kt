package com.techmarket.service

import com.techmarket.util.HealthCheckConstants.CLOSED_CONTENT_PATTERNS
import com.techmarket.util.HealthCheckConstants.Config.TIMEOUT_SECONDS
import com.techmarket.util.HealthCheckConstants.GENERIC_CAREERS_PATTERNS
import com.techmarket.util.HealthCheckConstants.HttpStatus.FOUND
import com.techmarket.util.HealthCheckConstants.HttpStatus.GONE
import com.techmarket.util.HealthCheckConstants.HttpStatus.INTERNAL_SERVER_ERROR
import com.techmarket.util.HealthCheckConstants.HttpStatus.NOT_FOUND
import com.techmarket.util.HealthCheckConstants.HttpStatus.OK
import com.techmarket.util.HealthCheckConstants.LOGIN_WALL_PATTERNS
import java.net.URI
import java.time.Duration
import org.slf4j.LoggerFactory
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono

/**
 * HTTP health checker for verifying job URL availability.
 * Uses HEAD requests for efficiency, falls back to GET when needed.
 */
@Component
class HttpHealthChecker(
    private val webClient: WebClient
) {

    private val log = LoggerFactory.getLogger(HttpHealthChecker::class.java)

    /**
     * Result of a health check on a job URL.
     */
    data class HealthCheckResult(
        val url: String,
        val status: UrlStatus,
        val httpStatusCode: Int?,
        val redirectUrl: String?,
        val failureReason: String?,
        val responseTimeMs: Long
    )

    enum class UrlStatus {
        ACTIVE,
        CLOSED_404,
        CLOSED_410,
        CLOSED_REDIRECT,
        CLOSED_NO_LONGER,
        CLOSED_FILLED,
        UNVERIFIED_LOGIN,
        UNVERIFIED_TIMEOUT,
        UNVERIFIED_ERROR
    }

    /**
     * Performs a health check on a job URL.
     * Uses HEAD request first (faster), falls back to GET if needed.
     */
    suspend fun checkUrl(url: String, jobTitle: String?): HealthCheckResult {
        val startTime = System.currentTimeMillis()

        try {
            // Try HEAD request first (lighter weight)
            val headResponse = webClient.method(HttpMethod.HEAD)
                .uri(url)
                .header("User-Agent", "DevAssembly-HealthChecker/1.0")
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(TIMEOUT_SECONDS))

            val statusCode = headResponse?.statusCode?.value()
            val location = headResponse?.headers?.location?.toString()

            return when {
                statusCode == OK -> {
                    // HEAD succeeded, but need to verify content with GET
                    verifyContentWithGet(url, jobTitle, startTime)
                }
                statusCode == NOT_FOUND -> HealthCheckResult(
                    url = url,
                    status = UrlStatus.CLOSED_404,
                    httpStatusCode = statusCode,
                    redirectUrl = null,
                    failureReason = "404 Not Found",
                    responseTimeMs = System.currentTimeMillis() - startTime
                )
                statusCode == GONE -> HealthCheckResult(
                    url = url,
                    status = UrlStatus.CLOSED_410,
                    httpStatusCode = statusCode,
                    redirectUrl = null,
                    failureReason = "410 Gone",
                    responseTimeMs = System.currentTimeMillis() - startTime
                )
                statusCode in 300..399 -> {
                    // Check if redirect is to generic careers page
                    if (isGenericCareersPage(location)) {
                        HealthCheckResult(
                            url = url,
                            status = UrlStatus.CLOSED_REDIRECT,
                            httpStatusCode = statusCode,
                            redirectUrl = location,
                            failureReason = "Redirects to generic careers page",
                            responseTimeMs = System.currentTimeMillis() - startTime
                        )
                    } else {
                        // Redirect might be valid, follow it
                        followRedirect(url, location, jobTitle, startTime)
                    }
                }
                statusCode != null && statusCode >= INTERNAL_SERVER_ERROR -> HealthCheckResult(
                    url = url,
                    status = UrlStatus.UNVERIFIED_ERROR,
                    httpStatusCode = statusCode,
                    redirectUrl = null,
                    failureReason = "Server error: $statusCode",
                    responseTimeMs = System.currentTimeMillis() - startTime
                )
                else -> HealthCheckResult(
                    url = url,
                    status = UrlStatus.UNVERIFIED_ERROR,
                    httpStatusCode = statusCode,
                    redirectUrl = null,
                    failureReason = "Unknown status",
                    responseTimeMs = System.currentTimeMillis() - startTime
                )
            }
        } catch (e: Exception) {
            return handleException(e, url, startTime)
        }
    }

    /**
     * Verifies content by fetching the page and checking for job-specific content.
     */
    private fun verifyContentWithGet(
        url: String,
        jobTitle: String?,
        startTime: Long
    ): HealthCheckResult {
        try {
            val body = webClient.get()
                .uri(url)
                .header("User-Agent", "DevAssembly-HealthChecker/1.0")
                .retrieve()
                .bodyToMono<String>()
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .block()

            if (body == null) {
                return HealthCheckResult(
                    url = url,
                    status = UrlStatus.UNVERIFIED_ERROR,
                    httpStatusCode = OK,
                    redirectUrl = null,
                    failureReason = "Empty response body",
                    responseTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Check for "no longer available" patterns
            val bodyLower = body.lowercase()
            for (pattern in CLOSED_CONTENT_PATTERNS) {
                if (bodyLower.contains(pattern)) {
                    return HealthCheckResult(
                        url = url,
                        status = UrlStatus.CLOSED_NO_LONGER,
                        httpStatusCode = OK,
                        redirectUrl = null,
                        failureReason = "Content indicates job closed: $pattern",
                        responseTimeMs = System.currentTimeMillis() - startTime
                    )
                }
            }

            // Check for login walls
            if (isLoginWall(body)) {
                return HealthCheckResult(
                    url = url,
                    status = UrlStatus.UNVERIFIED_LOGIN,
                    httpStatusCode = OK,
                    redirectUrl = null,
                    failureReason = "Requires login to view",
                    responseTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // Check if job title appears in page (if provided)
            if (jobTitle != null && !body.contains(jobTitle, ignoreCase = true)) {
                // Job title not found - might be closed or wrong page
                // Don't mark as closed immediately, but flag for review
                return HealthCheckResult(
                    url = url,
                    status = UrlStatus.ACTIVE,  // Still active, but log warning
                    httpStatusCode = OK,
                    redirectUrl = null,
                    failureReason = "Job title not found in page (warning only)",
                    responseTimeMs = System.currentTimeMillis() - startTime
                )
            }

            // All checks passed - URL is active
            return HealthCheckResult(
                url = url,
                status = UrlStatus.ACTIVE,
                httpStatusCode = OK,
                redirectUrl = null,
                failureReason = null,
                responseTimeMs = System.currentTimeMillis() - startTime
            )

        } catch (e: Exception) {
            return handleException(e, url, startTime)
        }
    }

    /**
     * Checks if a URL points to a generic careers page (not job-specific).
     */
    private fun isGenericCareersPage(url: String?): Boolean {
        if (url == null) return false

        val urlLower = url.lowercase()
        return GENERIC_CAREERS_PATTERNS.any { urlLower.contains(it) } &&
               !url.contains("?") &&  // No job ID parameters
               !url.matches(Regex(".*/job/.*"))  // No /job/ path segment
    }

    /**
     * Checks if response body indicates a login wall.
     */
    private fun isLoginWall(body: String): Boolean {
        val bodyLower = body.lowercase()
        return LOGIN_WALL_PATTERNS.any { bodyLower.contains(it) }
    }

    private fun handleException(e: Exception, url: String, startTime: Long): HealthCheckResult {
        return when (e) {
            is java.net.SocketTimeoutException,
            is java.util.concurrent.TimeoutException -> HealthCheckResult(
                url = url,
                status = UrlStatus.UNVERIFIED_TIMEOUT,
                httpStatusCode = null,
                redirectUrl = null,
                failureReason = "Timeout: ${e.message}",
                responseTimeMs = System.currentTimeMillis() - startTime
            )
            is java.net.UnknownHostException -> HealthCheckResult(
                url = url,
                status = UrlStatus.CLOSED_404,  // Domain doesn't exist
                httpStatusCode = null,
                redirectUrl = null,
                failureReason = "Unknown host: ${e.message}",
                responseTimeMs = System.currentTimeMillis() - startTime
            )
            else -> HealthCheckResult(
                url = url,
                status = UrlStatus.UNVERIFIED_ERROR,
                httpStatusCode = null,
                redirectUrl = null,
                failureReason = "Error: ${e.message}",
                responseTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun followRedirect(
        url: String,
        redirectUrl: String?,
        jobTitle: String?,
        startTime: Long
    ): HealthCheckResult {
        if (redirectUrl == null) {
            return HealthCheckResult(
                url = url,
                status = UrlStatus.UNVERIFIED_ERROR,
                httpStatusCode = FOUND,
                redirectUrl = null,
                failureReason = "Redirect with no location header",
                responseTimeMs = System.currentTimeMillis() - startTime
            )
        }

        if (isGenericCareersPage(redirectUrl)) {
            return HealthCheckResult(
                url = url,
                status = UrlStatus.CLOSED_REDIRECT,
                httpStatusCode = FOUND,
                redirectUrl = redirectUrl,
                failureReason = "Redirects to generic careers page: $redirectUrl",
                responseTimeMs = System.currentTimeMillis() - startTime
            )
        }

        // Follow the redirect and check content
        return verifyContentWithGet(redirectUrl, jobTitle, startTime)
    }
}
