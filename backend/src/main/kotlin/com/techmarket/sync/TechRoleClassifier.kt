package com.techmarket.sync

import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.sync.model.ApifyJobDto
import org.springframework.stereotype.Component

/**
 * Classifies whether a job posting is a "tech role" based on its title, department, and
 * description. This is used to filter out noise (HR, Finance, Legal) early in the pipeline.
 */
@Component
open class TechRoleClassifier(private val parser: RawJobDataParser) {

        private val techTitleKeywords =
                listOf(
                        "engineer",
                        "developer",
                        "architect",
                        "devops",
                        "sre",
                        "data scientist",
                        "analyst",
                        "designer",
                        "ux",
                        "ui",
                        "product manager",
                        "program manager",
                        "qa",
                        "tester",
                        "testing",
                        "security",
                        "cloud",
                        "platform",
                        "infrastructure",
                        "machine learning",
                        "ai",
                        "cto",
                        "cio",
                        "vpe",
                        "vpi",
                        "technical",
                        "software",
                        "fullstack",
                        "frontend",
                        "backend",
                        "mobile",
                        "android",
                        "ios",
                        "data",
                        "database",
                        "network",
                        "systems",
                        "support",
                        "hardware",
                        "firmware",
                        "embedded",
                        "electronics",
                        "rf",
                        "avionics",
                        "robotics",
                        "mechatronics",
                        "automation",
                        "technician",
                        "v&v",
                        "verification",
                        "validation",
                        "integration",
                        "consultant",
                        "salesforce",
                        "sap",
                        "servicenow"
                )

        private val techDepartmentKeywords =
                listOf(
                        "engineering",
                        "product",
                        "design",
                        "it",
                        "data",
                        "security",
                        "platform",
                        "infrastructure",
                        "r&d",
                        "research",
                        "operations",
                        "test",
                        "quality",
                        "software",
                        "hardware",
                        "systems",
                        "technology",
                        "information technology"
                )

        private val nonTechTitleKeywords =
                listOf(
                        "accountant",
                        "recruiter",
                        "hr",
                        "human resources",
                        "people",
                        "talent",
                        "receptionist",
                        "nurse",
                        "paralegal",
                        "lawyer",
                        "attorney",
                        "sales",
                        "marketing",
                        "facilities",
                        "janitor",
                        "cleaner"
                )

        /** Entry point for Greenhouse/ATS flow. */
        open fun isTechRole(job: NormalizedJob): Boolean {
                return isTechRole(
                        title = job.title,
                        department = job.department,
                        description = job.descriptionText ?: ""
                )
        }

        /** Entry point for Apify/Scraped flow. */
        open fun isTechRole(dto: ApifyJobDto): Boolean {
                return isTechRole(
                        title = dto.title,
                        department = dto.jobFunction, // Closest match in Apify DTO
                        description = dto.descriptionText ?: ""
                )
        }

        /** Generic classification logic. */
        open fun isTechRole(title: String?, department: String?, description: String): Boolean {
                val t = title?.lowercase() ?: ""
                val d = department?.lowercase() ?: ""

                // 1. Explicit Non-Tech Check (Fast-Fail)
                val sanitizedTitle = t.replace("/hr", "/hour").replace("/ hr", "/hour")
                if (nonTechTitleKeywords.any { Regex("\\b$it\\b").containsMatchIn(sanitizedTitle) }) return false

                // 2. Clear Tech Signal from Title
                val titleMatch = techTitleKeywords.any { t.contains(it) }
                if (titleMatch) return true

                // 3. Clear Tech Signal from Department
                // Use a word boundary specifically for small words like 'it'
                if (techDepartmentKeywords.any { 
                    if (it == "it") Regex("\\b$it\\b").containsMatchIn(d) 
                    else d.contains(it) 
                }) return true

                // 4. Ambiguous Title Rescue via Tech Keywords Density
                // If we find 2 or more recognized tech stacks in the description, count it as tech.
                val techCount = parser.extractTechnologies(description).size
                return techCount >= 2
        }
}
