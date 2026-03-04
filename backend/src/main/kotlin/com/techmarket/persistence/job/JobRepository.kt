package com.techmarket.persistence.job

import com.techmarket.persistence.model.JobRecord

interface JobRepository {
    fun saveJobs(jobs: List<JobRecord>)
    fun deleteAllJobs()
    fun getJobDetails(jobId: String): com.techmarket.api.model.JobPageDto?
    fun getJobsByIds(jobIds: List<String>): List<JobRecord>
    fun getAllJobs(): List<JobRecord>
    fun deleteJobsByIds(jobIds: List<String>)
}
