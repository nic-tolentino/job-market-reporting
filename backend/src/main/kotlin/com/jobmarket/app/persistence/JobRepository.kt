package com.jobmarket.app.persistence

import com.jobmarket.app.api.model.*
import com.jobmarket.app.dashboard.model.TechTrendDto
import com.jobmarket.app.persistence.model.CompanyRecord
import com.jobmarket.app.persistence.model.JobRecord
import com.jobmarket.app.persistence.model.RawIngestionRecord

interface JobRepository {
    fun saveRawIngestions(records: List<RawIngestionRecord>)
    fun saveJobs(jobs: List<JobRecord>)
    fun saveCompanies(companies: List<CompanyRecord>)
    fun getTechTrendsByWeek(monthsBack: Int): List<TechTrendDto>

    fun getLandingPageData(): LandingPageDto
    fun getTechDetails(techName: String): TechDetailsPageDto
    fun getCompanyProfile(companyId: String): CompanyProfilePageDto
}
