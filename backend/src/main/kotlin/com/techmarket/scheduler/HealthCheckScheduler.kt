package com.techmarket.scheduler

import com.techmarket.service.JobHealthCheckService
import com.techmarket.util.HealthCheckConstants.Scheduler.CRON_DAILY_2AM_UTC
import com.techmarket.util.HealthCheckConstants.Scheduler.TIMEZONE_UTC
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * Scheduler for running periodic job URL health checks.
 */
@Component
class HealthCheckScheduler(
    private val healthCheckService: JobHealthCheckService
) {

    private val log = LoggerFactory.getLogger(HealthCheckScheduler::class.java)

    /**
     * Runs daily at 2:00 AM UTC (2:00 PM NZST / 12:00 PM AEST).
     * This is off-peak for all our target markets.
     */
    @Scheduled(
        cron = CRON_DAILY_2AM_UTC,
        zone = TIMEZONE_UTC
    )
    fun runDailyHealthCheck() {
        log.info("Starting scheduled daily health check")

        try {
            runBlocking {
                val summary = healthCheckService.runHealthCheck()
                log.info("Daily health check completed: $summary")
            }
        } catch (e: Exception) {
            log.error("Daily health check failed: ${e.message}", e)
        }
    }
}
