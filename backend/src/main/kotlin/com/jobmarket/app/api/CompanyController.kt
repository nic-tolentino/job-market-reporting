package com.jobmarket.app.api

import com.jobmarket.app.api.model.*
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/company")
class CompanyController {

        @GetMapping("/{companyId}")
        fun getCompanyProfile(@PathVariable companyId: String): CompanyProfilePageDto {
                // Simple mock fallback based on ID
                val name =
                        companyId.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase() else it.toString()
                        }
                val logo = name.first().toString()

                return CompanyProfilePageDto(
                        companyDetails =
                                CompanyDetailsDto(
                                        id = companyId,
                                        name = name,
                                        logo = logo,
                                        website = "https://www.$companyId.com",
                                        employeesCount = 10000,
                                        industry = "Technology",
                                        description =
                                                "$name is a global technology leader building tools for modern teams."
                                ),
                        techStack = listOf("Kotlin", "Spring Boot", "PostgreSQL", "AWS", "Docker"),
                        insights =
                                CompanyInsightsDto(
                                        workModel = "Hybrid (2-3 days in office)",
                                        topHubs = "Sydney, San Francisco, Remote",
                                        commonBenefits =
                                                listOf(
                                                        "Health Insurance",
                                                        "Equity",
                                                        "Flexible Hours",
                                                        "Learning Budget"
                                                )
                                ),
                        activeRoles =
                                listOf(
                                        JobRoleDto(
                                                id = "1",
                                                title = "Backend Engineer (Kotlin)",
                                                companyId = companyId,
                                                companyName = name,
                                                location = "Sydney, AU (Hybrid)",
                                                salaryMin = 160000,
                                                salaryMax = 210000,
                                                postedDate = "2024-05-15",
                                                technologies =
                                                        listOf(
                                                                "Kotlin",
                                                                "Spring Boot",
                                                                "PostgreSQL"
                                                        )
                                        ),
                                        JobRoleDto(
                                                id = "2",
                                                title = "DevOps Specialist",
                                                companyId = companyId,
                                                companyName = name,
                                                location = "Remote, AU",
                                                salaryMin = 140000,
                                                salaryMax = 180000,
                                                postedDate = "2024-05-14",
                                                technologies = listOf("AWS", "Kubernetes", "Docker")
                                        )
                                )
                )
        }
}
