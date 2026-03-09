package com.techmarket.util

/**
 * Constants for Cloud Tasks processing.
 */
object CloudTasksConstants {

    /**
     * Source types for sync tasks.
     */
    object Source {
        const val APIFY = "APIFY"
        const val ATS = "ATS"
        const val MANUAL = "MANUAL"
        const val SCHEDULED = "SCHEDULED"
    }

    /**
     * Trigger types for sync tasks.
     */
    object TriggeredBy {
        const val WEBHOOK = "WEBHOOK"
        const val SCHEDULED = "SCHEDULED"
        const val ADMIN = "ADMIN"
    }

    /**
     * Cloud Tasks header names.
     */
    const val HEADER_CLOUD_TASKS = "X-Cloud-Tasks"
    const val HEADER_CLOUD_TASKS_TASK_NAME = "X-Cloud-Tasks-Task-Name"

    /**
     * Queue names.
     */
    const val QUEUE_SYNC = "tech-market-sync-queue"
    const val QUEUE_DLQ = "tech-market-sync-dlq"
}
