package com.techmarket.sync

import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.sync.model.ApifyJobDto
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TechRoleClassifierTest {

    private val parser = mockk<RawJobDataParser>(relaxed = true)
    private val classifier = TechRoleClassifier(parser)

    @Test
    fun `should classify common tech titles as tech`() {
        val titles =
                listOf(
                        "Software Engineer",
                        "DevOps Engineer",
                        "Data Scientist",
                        "Product Designer",
                        "UX/UI Specialist",
                        "QA Tester",
                        "Security Architect",
                        "CTO",
                        "VPE",
                        "Backend Developer",
                        "Frontend Engineer",
                        "Cloud Platform Engineer"
                )

        titles.forEach { title ->
            assertTrue(
                    classifier.isTechRole(title, null, ""),
                    "Expected '$title' to be a tech role"
            )
        }
    }

    @Test
    fun `should classify non-tech titles as non-tech`() {
        val titles =
                listOf(
                        "Accountant",
                        "HR Manager",
                        "Legal Counsel",
                        "Office Administrator",
                        "Sales Representative",
                        "Marketing Specialist",
                        "Receptionist",
                        "Nurse"
                )

        titles.forEach { title ->
            assertFalse(
                    classifier.isTechRole(title, null, ""),
                    "Expected '$title' NOT to be a tech role"
            )
        }
    }

    @Test
    fun `should use department as secondary signal`() {
        // Obscure title but clear tech department
        assertTrue(classifier.isTechRole("Lead Guru", "Engineering", ""))
        assertTrue(classifier.isTechRole("Specialist", "Data", ""))

        // Clear non-tech department despite ambiguous title
        assertFalse(classifier.isTechRole("Specialist", "Finance", ""))
        assertFalse(classifier.isTechRole("Manager", "Human Resources", ""))
    }

    @Test
    fun `should rescue ambiguous titles via tech keyword density`() {
        val description = "We use Kotlin, Spring Boot, and PostgreSQL."
        every { parser.extractTechnologies(description) } returns
                listOf("Kotlin", "Spring Boot", "PostgreSQL")

        // "Project Manager" is ambiguous, but description has 3 tech keywords
        assertTrue(classifier.isTechRole("Project Manager", null, description))

        // "Manager" with 0 tech keywords remains non-tech
        every { parser.extractTechnologies("no tech here") } returns emptyList()
        assertFalse(classifier.isTechRole("Manager", null, "no tech here"))
    }

    @Test
    fun `should handle NormalizedJob entry point`() {
        val job = createNormalizedJob("Site Reliability Engineer", "Platform")
        assertTrue(classifier.isTechRole(job))
    }

    @Test
    fun `should handle ApifyJobDto entry point`() {
        val description = "We use React and Node."
        val dto = createApifyDto("Product Manager", "Engineering", description)
        every { parser.extractTechnologies(description) } returns listOf("React", "Node.js")

        assertTrue(classifier.isTechRole(dto))
    }

    private fun createNormalizedJob(title: String, department: String?) =
            NormalizedJob(
                    platformId = "123",
                    source = "Greenhouse",
                    title = title,
                    companyName = "TechCorp",
                    location = "London",
                    descriptionHtml = null,
                    descriptionText = "Some text",
                    salaryMin = null,
                    salaryMax = null,
                    salaryCurrency = null,
                    employmentType = null,
                    seniorityLevel = null,
                    workModel = null,
                    department = department,
                    postedAt = null,
                    firstPublishedAt = null,
                    applyUrl = null,
                    platformUrl = null,
                    rawPayload = ""
            )

    private fun createApifyDto(title: String, jobFunction: String?, description: String) =
            ApifyJobDto(
                    id = "abc",
                    title = title,
                    companyName = "Acme",
                    companyLogo = null,
                    location = "London",
                    salaryInfo = null,
                    postedAt = "2023-01-01",
                    benefits = null,
                    applicantsCount = null,
                    applyUrl = null,
                    descriptionHtml = null,
                    descriptionText = description,
                    link = null,
                    seniorityLevel = null,
                    employmentType = null,
                    jobFunction = jobFunction,
                    industries = null,
                    companyDescription = null,
                    companyWebsite = null,
                    companyEmployeesCount = null
            )
}
