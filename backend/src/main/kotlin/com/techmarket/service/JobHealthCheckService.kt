package com.techmarket.service

import com.techmarket.persistence.job.JobRepository
import com.techmarket.util.HealthCheckConstants
import com.techmarket.util.HealthCheckConstants.Config.ALERT_FAILURE_RATE_THRESHOLD
import com.techmarket.util.HealthCheckConstants.Config.MAX_CONCURRENT_CHECKS
import com.techmarket.util.HealthCheckConstants.Config.MAX_FAILURES_BEFORE_UNVERIFIED
import com.techmarket.util.HealthCheckConstants.UrlStatus.isClosed
import com.techmarket.util.HealthCheckConstants.UrlStatus.isUnverified
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

/**
 * Orchestrates health checks on job URLs.
 * Runs periodically to detect dead links and mark jobs as closed.
 */
@Service
class JobHealthCheckService(
    private val jobRepository: JobRepository,
    private val httpHealthChecker: HttpHealthChecker
) {

    private val log = LoggerFactory.getLogger(JobHealthCheckService::class.java)

    /**
     * Runs health checks on all jobs needing verification.
     * Returns summary statistics.
     */
    suspend fun runHealthCheck(): HealthCheckSummary {
        val jobsToCheck = jobRepository.getJobsNeedingHealthCheck(
            limit = 1000
        )
        log.info("Starting health check for ${jobsToCheck.size} jobs")

        if (jobsToCheck.isEmpty()) {
            log.info("No jobs need health checking")
            return HealthCheckSummary(
                totalChecked = 0,
                activeCount = 0,
                closedCount = 0,
                unverifiedCount = 0,
                avgResponseTimeMs = 0.0,
                durationSeconds = 0
            )
        }

        val startTime = Instant.now()
        val results = mutableListOf<HttpHealthChecker.HealthCheckResult>()

        // Process in parallel with concurrency limit using semaphore
        val semaphore = Semaphore(MAX_CONCURRENT_CHECKS)

        withContext(Dispatchers.IO) {
            jobsToCheck.map { job ->
                async {
                    semaphore.acquire()
                    try {
                        // Get first apply URL for health check
                        val url = job.applyUrls.firstOrNull() ?: return@async
                        val result = httpHealthChecker.checkUrl(
                            url = url,
                            jobTitle = job.title
                        )
                        results.add(result)

                        // Update job record in BigQuery
                        updateJobHealth(job.jobId, result)

                    } catch (e: Exception) {
                        log.error("Health check failed for job ${job.jobId}: ${e.message}")
                    } finally {
                        semaphore.release()
                    }
                }
            }.awaitAll()
        }

        val summary = HealthCheckSummary(
            totalChecked = results.size,
            activeCount = results.count { it.status == HttpHealthChecker.UrlStatus.ACTIVE },
            closedCount = results.count { isClosed(it.status.name) },
            unverifiedCount = results.count { isUnverified(it.status.name) },
            avgResponseTimeMs = results.map { it.responseTimeMs }.average(),
            durationSeconds = Duration.between(startTime, Instant.now()).seconds
        )

        log.info("Health check completed: $summary")

        // Alert if high failure rate
        if (summary.totalChecked > 0) {
            val failureRate = (summary.closedCount + summary.unverifiedCount).toDouble() / summary.totalChecked
            if (failureRate > ALERT_FAILURE_RATE_THRESHOLD) {
                log.warn("⚠️ High job URL failure rate: ${(failureRate * 100).toInt()}% " +
                        "(${summary.closedCount} closed, ${summary.unverifiedCount} unverified)")
                // In production, this would send a Slack/email alert
            }
        }

        return summary
    }

    private suspend fun updateJobHealth(jobId: String, result: HttpHealthChecker.HealthCheckResult) {
        val now = Instant.now()

        when (result.status) {
            HttpHealthChecker.UrlStatus.ACTIVE -> {
                jobRepository.updateJobUrlHealth(
                    jobId = jobId,
                    urlStatus = HealthCheckConstants.UrlStatus.ACTIVE,
                    httpStatusCode = result.httpStatusCode,
                    urlLastChecked = now,
                    urlCheckFailures = 0,
                    urlLastKnownActive = now
                )
            }

            in listOf(
                HttpHealthChecker.UrlStatus.CLOSED_404,
                HttpHealthChecker.UrlStatus.CLOSED_410,
                HttpHealthChecker.UrlStatus.CLOSED_REDIRECT,
                HttpHealthChecker.UrlStatus.CLOSED_NO_LONGER,
                HttpHealthChecker.UrlStatus.CLOSED_FILLED
            ) -> {
                // Mark job as closed
                jobRepository.markJobAsClosed(
                    jobId = jobId,
                    reason = result.status.name,
                    closedAt = now
                )
            }

            else -> {
                // Unverified - increment failure counter
                val currentJob = jobRepository.getJobById(jobId)
                val newFailureCount = (currentJob?.urlCheckFailures ?: 0) + 1

                if (newFailureCount >= MAX_FAILURES_BEFORE_UNVERIFIED.toInt()) {
                    // After 3 consecutive failures, mark as unverified
                    jobRepository.updateJobUrlHealth(
                        jobId = jobId,
                        urlStatus = result.status.name,
                        httpStatusCode = result.httpStatusCode,
                        urlLastChecked = now,
                        urlCheckFailures = newFailureCount,
                        urlLastKnownActive = null
                    )
                } else {
                    // Keep as active but track failures
                    jobRepository.updateJobUrlHealth(
                        jobId = jobId,
                        urlStatus = HealthCheckConstants.UrlStatus.ACTIVE,
                        httpStatusCode = result.httpStatusCode,
                        urlLastChecked = now,
                        urlCheckFailures = newFailureCount,
                        urlLastKnownActive = currentJob?.urlLastKnownActive
                    )
                }
            }
        }
    }

    data class HealthCheckSummary(
        val totalChecked: Int,
        val activeCount: Int,
        val closedCount: Int,
        val unverifiedCount: Int,
        val avgResponseTimeMs: Double,
        val durationSeconds: Long
    ) {
        override fun toString(): String {
            return "HealthCheckSummary(total=$totalChecked, active=$activeCount, " +
                   "closed=$closedCount, unverified=$unverifiedCount, " +
                   "avgTime=${avgResponseTimeMs.toInt()}ms, duration=${durationSeconds}s)"
        }
    }
}
