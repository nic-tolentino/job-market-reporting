package com.techmarket.persistence.company

import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.persistence.model.CompanyRecord

interface CompanyRepository {
    fun saveCompanies(companies: List<CompanyRecord>)
    fun deleteAllCompanies()
    fun getCompanyProfile(companyId: String): CompanyProfilePageDto
    fun getAllCompanies(): List<CompanyRecord>
    fun getCompaniesByIds(companyIds: List<String>): List<CompanyRecord>
    fun deleteCompaniesByIds(companyIds: List<String>)
}
