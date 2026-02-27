package com.techmarket.persistence.job

import com.techmarket.persistence.model.JobRecord

interface JobRepository {
    fun saveJobs(jobs: List<JobRecord>)
    fun deleteAllJobs()
    fun getJobDetails(jobId: String): com.techmarket.api.model.JobPageDto?
}
