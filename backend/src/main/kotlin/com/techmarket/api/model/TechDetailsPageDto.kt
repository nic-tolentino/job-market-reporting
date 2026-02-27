package com.techmarket.api.model

data class TechDetailsPageDto(
        val techName: String,
        val totalJobs: Int,
        val seniorityDistribution: List<SeniorityDistributionDto>,
        val hiringCompanies: List<CompanyLeaderboardDto>
)

data class SeniorityDistributionDto(val name: String, val value: Int)
