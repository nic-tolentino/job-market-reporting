package com.techmarket.persistence.company

import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.models.CompanyListingItem
import com.techmarket.persistence.model.CompanyRecord

interface CompanyRepository {
    fun saveCompanies(companies: List<CompanyRecord>)
    fun deleteAllCompanies()
    fun getCompanyProfile(companyId: String, country: String? = null): CompanyProfilePageDto
    fun getAllCompanies(): List<CompanyRecord>
    fun getCompaniesByIds(companyIds: List<String>): List<CompanyRecord>
    fun deleteCompaniesByIds(companyIds: List<String>)
    
    // Phase B Listing
    fun getCompanyListing(
        visaOnly: Boolean = false,
        country: String? = null,
        limit: Int = 50,
        offset: Int = 0
    ): List<CompanyListingItem>
}
