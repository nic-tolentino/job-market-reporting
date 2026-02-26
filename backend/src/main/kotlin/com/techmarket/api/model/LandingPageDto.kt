package com.techmarket.api.model

data class LandingPageDto(
        val globalStats: GlobalStatsDto,
        val topTech: List<TechTrendAggregatedDto>,
        val topCompanies: List<CompanyLeaderboardDto>
)

data class GlobalStatsDto(
        val totalVacancies: Int,
        val remotePercentage: Int,
        val hybridPercentage: Int,
        val topTech: String
)

data class TechTrendAggregatedDto(val name: String, val count: Int, val percentageChange: Double)
