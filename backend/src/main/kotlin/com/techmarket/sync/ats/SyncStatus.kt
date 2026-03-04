package com.techmarket.sync.ats

/** Represents the status of a specific company's ATS synchronization attempt. */
enum class SyncStatus {
    SUCCESS,
    FAILED,
    AUTH_FAILED,
    PENDING
}
