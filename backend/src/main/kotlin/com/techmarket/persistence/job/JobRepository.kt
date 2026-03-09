package com.techmarket.persistence.job

import com.techmarket.api.model.JobPageDto
import com.techmarket.persistence.model.JobRecord
import java.time.Instant

interface JobRepository {
    fun saveJobs(jobs: List<JobRecord>)
    fun deleteAllJobs()
    fun getJobDetails(jobId: String): JobPageDto?
    fun getJobsByIds(jobIds: List<String>): List<JobRecord>
    fun getAllJobs(): List<JobRecord>
    fun deleteJobsByIds(jobIds: List<String>)

    // Health check methods for dead link detection
    /**
     * Gets all active jobs that need health checking.
     * Returns jobs not checked in the last 24 hours.
     */
    fun getJobsNeedingHealthCheck(limit: Int = 1000): List<JobRecord>

    /**
     * Updates the URL health status for a job.
     */
    fun updateJobUrlHealth(
        jobId: String,
        urlStatus: String,
        httpStatusCode: Int?,
        urlLastChecked: Instant,
        urlCheckFailures: Int,
        urlLastKnownActive: Instant? = null
    )

    /**
     * Marks a job as closed due to URL issues.
     */
    fun markJobAsClosed(jobId: String, reason: String, closedAt: Instant)

    /**
     * Gets a single job by ID.
     */
    fun getJobById(jobId: String): JobRecord?
}
