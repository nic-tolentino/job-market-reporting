package com.techmarket.util

/**
 * Constants for job URL health checks.
 */
object HealthCheckConstants {

    /**
     * URL status values stored in BigQuery.
     */
    object UrlStatus {
        const val ACTIVE = "ACTIVE"
        const val CLOSED_404 = "CLOSED_404"
        const val CLOSED_410 = "CLOSED_410"
        const val CLOSED_REDIRECT = "CLOSED_REDIRECT"
        const val CLOSED_NO_LONGER = "CLOSED_NO_LONGER"
        const val CLOSED_FILLED = "CLOSED_FILLED"
        const val UNVERIFIED_LOGIN = "UNVERIFIED_LOGIN"
        const val UNVERIFIED_TIMEOUT = "UNVERIFIED_TIMEOUT"
        const val UNVERIFIED_ERROR = "UNVERIFIED_ERROR"
        const val UNKNOWN = "UNKNOWN"

        /**
         * Check if a status indicates the job is closed.
         */
        fun isClosed(status: String?): Boolean {
            return status?.startsWith("CLOSED_") == true
        }

        /**
         * Check if a status indicates the job is unverified.
         */
        fun isUnverified(status: String?): Boolean {
            return status?.startsWith("UNVERIFIED_") == true
        }
    }

    /**
     * HTTP status codes commonly encountered.
     */
    object HttpStatus {
        const val OK = 200
        const val FOUND = 302
        const val MOVED_PERMANENTLY = 301
        const val NOT_FOUND = 404
        const val GONE = 410
        const val INTERNAL_SERVER_ERROR = 500
    }

    /**
     * Content patterns that indicate a job is closed.
     */
    val CLOSED_CONTENT_PATTERNS = listOf(
        "no longer available",
        "position filled",
        "position closed",
        "job expired",
        "this role is no longer",
        "we've filled this position"
    )

    /**
     * Content patterns that indicate a login wall.
     */
    val LOGIN_WALL_PATTERNS = listOf(
        "sign in to continue",
        "log in to view",
        "please log in",
        "authentication required",
        "access denied",
        "you must be logged in"
    )

    /**
     * URL patterns that indicate a generic careers page.
     */
    val GENERIC_CAREERS_PATTERNS = listOf(
        "/careers",
        "/jobs",
        "/work-with-us",
        "/join-us",
        "/opportunities",
        "/life-at",
        "careers.",
        "jobs."
    )

    /**
     * Scheduler configuration.
     */
    object Scheduler {
        const val CRON_DAILY_2AM_UTC = "0 0 2 * * *"
        const val TIMEZONE_UTC = "UTC"
    }

    /**
     * Health check configuration.
     */
    object Config {
        const val MAX_CONCURRENT_CHECKS = 20
        const val TIMEOUT_SECONDS = 10L
        const val MAX_FAILURES_BEFORE_UNVERIFIED = 3L
        const val HOURS_BETWEEN_CHECKS = 24
        const val SECONDS_IN_DAY = 86400L
        const val ALERT_FAILURE_RATE_THRESHOLD = 0.1  // 10%
    }

    /**
     * API endpoint paths.
     */
    object Endpoints {
        const val ADMIN_HEALTH_CHECK_RUN = "/api/admin/health-check/run"
        const val ADMIN_HEALTH_CHECK_STATS = "/api/admin/health-check/stats"
    }
}
