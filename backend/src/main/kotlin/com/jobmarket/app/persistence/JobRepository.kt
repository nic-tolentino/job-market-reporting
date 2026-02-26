package com.jobmarket.app.persistence

import com.jobmarket.app.api.model.CompanyProfilePageDto
import com.jobmarket.app.api.model.LandingPageDto
import com.jobmarket.app.api.model.SearchSuggestionsResponse
import com.jobmarket.app.api.model.TechDetailsPageDto
import com.jobmarket.app.persistence.model.CompanyRecord
import com.jobmarket.app.persistence.model.JobRecord
import com.jobmarket.app.persistence.model.RawIngestionRecord

interface JobRepository {
    fun saveRawIngestions(records: List<RawIngestionRecord>)
    fun saveJobs(jobs: List<JobRecord>)
    fun saveCompanies(companies: List<CompanyRecord>)

    fun getLandingPageData(): LandingPageDto
    fun getTechDetails(techName: String): TechDetailsPageDto
    fun getCompanyProfile(companyId: String): CompanyProfilePageDto

    fun getSearchSuggestions(): SearchSuggestionsResponse
    fun saveSearchMiss(term: String)
    fun saveFeedback(context: String?, message: String)
}
