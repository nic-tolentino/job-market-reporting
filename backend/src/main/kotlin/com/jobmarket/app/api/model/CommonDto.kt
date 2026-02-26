package com.jobmarket.app.api.model

data class CompanyLeaderboardDto(
        val id: String,
        val name: String,
        val logo: String,
        val activeRoles: Int
)

data class JobRoleDto(
        val id: String,
        val title: String,
        val companyId: String,
        val companyName: String,
        val location: String,
        val salaryMin: Int?,
        val salaryMax: Int?,
        val postedDate: String,
        val technologies: List<String>
)
