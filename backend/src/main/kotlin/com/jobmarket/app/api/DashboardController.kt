package com.jobmarket.app.api

import com.jobmarket.app.api.model.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
class BffDashboardController {

    @GetMapping("/landing")
    fun getLandingPageData(): LandingPageDto {
        return LandingPageDto(
                globalStats =
                        GlobalStatsDto(
                                totalVacancies = 4281,
                                remotePercentage = 34,
                                hybridPercentage = 45,
                                topTech = "Kotlin"
                        ),
                topTech =
                        listOf(
                                TechTrendAggregatedDto("Kotlin", 1205, 12.5),
                                TechTrendAggregatedDto("Java", 1150, -5.0),
                                TechTrendAggregatedDto("Go", 850, 4.8),
                                TechTrendAggregatedDto("Node.js", 820, -2.4),
                                TechTrendAggregatedDto("Python", 450, 1.2),
                                TechTrendAggregatedDto("C#", 410, 2.1),
                                TechTrendAggregatedDto("Ruby", 380, -3.2)
                        ),
                topCompanies =
                        listOf(
                                CompanyLeaderboardDto("atlassian", "Atlassian", "A", 145),
                                CompanyLeaderboardDto("canva", "Canva", "C", 89),
                                CompanyLeaderboardDto("google", "Google", "G", 76),
                                CompanyLeaderboardDto("amazon", "Amazon", "Am", 65),
                                CompanyLeaderboardDto("xero", "Xero", "X", 54)
                        )
        )
    }
}
