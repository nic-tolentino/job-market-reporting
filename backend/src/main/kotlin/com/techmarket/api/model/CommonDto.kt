package com.techmarket.api.model

data class CompanyLeaderboardDto(
        val id: String,
        val name: String,
        val logo: String,
        val activeRoles: Int
)

data class JobRoleDto(
        val id: String, // canonical ID (first jobId in the group)
        val title: String,
        val companyId: String,
        val companyName: String,
        val locations: List<String>, // all locations this role is advertised in
        val jobIds: List<String>, // original LinkedIn job IDs per location
        val applyUrls: List<String?>, // apply URLs per location
        val salaryMin: Int?,
        val salaryMax: Int?,
        val postedDate: String,
        val technologies: List<String>
)
