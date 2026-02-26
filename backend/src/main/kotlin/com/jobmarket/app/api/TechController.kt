package com.jobmarket.app.api

import com.jobmarket.app.api.model.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/tech")
class TechController {

        @GetMapping("/{techName}")
        fun getTechDetails(@PathVariable techName: String): TechDetailsPageDto {
                // Formatting to ensure consistency with mock frontend display
                val formattedName =
                        techName.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                        }

                return TechDetailsPageDto(
                        techName = formattedName,
                        seniorityDistribution =
                                listOf(
                                        SeniorityDistributionDto("Senior", 400),
                                        SeniorityDistributionDto("Mid-Level", 300),
                                        SeniorityDistributionDto("Junior", 100),
                                        SeniorityDistributionDto("Lead/Principal", 50)
                                ),
                        hiringCompanies =
                                listOf(
                                        CompanyLeaderboardDto(
                                                "atlassian",
                                                "Atlassian",
                                                "A",
                                                (145 * 0.3).toInt()
                                        ),
                                        CompanyLeaderboardDto(
                                                "canva",
                                                "Canva",
                                                "C",
                                                (89 * 0.3).toInt()
                                        ),
                                        CompanyLeaderboardDto(
                                                "google",
                                                "Google",
                                                "G",
                                                (76 * 0.3).toInt()
                                        ),
                                        CompanyLeaderboardDto(
                                                "amazon",
                                                "Amazon",
                                                "Am",
                                                (65 * 0.3).toInt()
                                        ),
                                        CompanyLeaderboardDto(
                                                "xero",
                                                "Xero",
                                                "X",
                                                (54 * 0.3).toInt()
                                        )
                                )
                )
        }
}
