package com.jobmarket.app.persistence

import com.jobmarket.app.dashboard.model.TechTrendDto
import com.jobmarket.app.persistence.model.CompanyRecord
import com.jobmarket.app.persistence.model.JobRecord

interface JobRepository {
    fun saveJobs(jobs: List<JobRecord>)
    fun saveCompanies(companies: List<CompanyRecord>)
    fun getTechTrendsByWeek(monthsBack: Int): List<TechTrendDto>
}
