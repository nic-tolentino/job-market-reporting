package com.techmarket.persistence

import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.persistence.model.CompanyRecord

interface CompanyRepository {
    fun saveCompanies(companies: List<CompanyRecord>)
    fun deleteAllCompanies()
    fun getCompanyProfile(companyId: String): CompanyProfilePageDto
}
