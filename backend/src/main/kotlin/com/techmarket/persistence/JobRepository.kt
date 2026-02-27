package com.techmarket.persistence

import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.api.model.LandingPageDto
import com.techmarket.api.model.SearchSuggestionsResponse
import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.persistence.model.RawIngestionRecord

interface JobRepository {
    fun saveRawIngestions(records: List<RawIngestionRecord>)
    fun getRawIngestions(): List<RawIngestionRecord>
    fun saveJobs(jobs: List<JobRecord>)
    fun deleteAllJobs()
    fun saveCompanies(companies: List<CompanyRecord>)
    fun deleteAllCompanies()

    fun getLandingPageData(): LandingPageDto
    fun getTechDetails(techName: String): TechDetailsPageDto
    fun getCompanyProfile(companyId: String): CompanyProfilePageDto
    fun getJobDetails(jobId: String): com.techmarket.api.model.JobPageDto?

    fun getSearchSuggestions(): SearchSuggestionsResponse
    fun saveSearchMiss(term: String)
    fun saveFeedback(context: String?, message: String)
}
