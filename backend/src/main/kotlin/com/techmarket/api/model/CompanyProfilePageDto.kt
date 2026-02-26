package com.techmarket.api.model

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
        val description: String
)

data class CompanyInsightsDto(
        val workModel: String,
        val topHubs: String,
        val commonBenefits: List<String>
)
