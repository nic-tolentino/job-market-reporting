package com.techmarket.api.model

import com.techmarket.persistence.model.VerificationLevel

data class CompanyProfilePageDto(
    val companyDetails: CompanyDetailsDto,
    val techStack: List<String>,
    val insights: CompanyInsightsDto,
    val activeRoles: List<JobRoleDto>
)

data class CompanyDetailsDto(
    val id: String,
    val name: String,
    val logo: String,
    val website: String,
    val employeesCount: Int,
    val industry: String,
    val description: String,
    val isAgency: Boolean = false,
    val isSocialEnterprise: Boolean = false,
    val hqCountry: String? = null,
    val remotePolicy: String? = null,
    val visaSponsorship: Boolean = false,
    val verificationLevel: VerificationLevel = VerificationLevel.VERIFIED
)

data class CompanyInsightsDto(
        val workModel: String,
        val hiringLocations: List<String>,
        val commonBenefits: List<String>,
        val operatingCountries: List<String> = emptyList(),
        val officeLocations: List<String> = emptyList()
)
