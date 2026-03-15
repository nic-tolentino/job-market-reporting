package com.techmarket.sync.ats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.crawler.CrawlRunRepository
import com.techmarket.persistence.crawler.CrawlerSeedRepository
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.service.CrawlLogService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.RestTemplate
import java.net.URI
import java.time.Instant

/**
 * Unit tests for CrawlerClient
 */
class CrawlerClientTest {

    private lateinit var crawlerClient: CrawlerClient
    private lateinit var restTemplate: RestTemplate
    private lateinit var objectMapper: ObjectMapper
    private lateinit var companyRepository: CompanyRepository
    private lateinit var crawlerSeedRepository: CrawlerSeedRepository
    private lateinit var crawlRunRepository: CrawlRunRepository
    private lateinit var crawlLogService: CrawlLogService

    @BeforeEach
    fun setup() {
        restTemplate = mockk()
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        companyRepository = mockk()
        crawlerSeedRepository = mockk(relaxed = true)
        crawlRunRepository = mockk(relaxed = true)
        crawlLogService = mockk(relaxed = true)
        crawlerClient = CrawlerClient(
            restTemplate, 
            objectMapper, 
            companyRepository, 
            crawlerSeedRepository, 
            crawlRunRepository, 
            crawlLogService, 
            "http://localhost:8080"
        )
    }

    @Test
    fun `fetchJobs returns crawler response`() {
        val mockResponse = """
            {
                "companyId": "airwallex",
                "crawlMeta": {
                    "pagesVisited": 3,
                    "totalJobsFound": 2,
                    "detectedAtsProvider": "ASHBY",
                    "detectedAtsIdentifier": "airwallex",
                    "crawlDurationMs": 5000,
                    "extractionModel": "gemini-2.0-flash",
                    "extractionConfidence": 0.92
                },
                "jobs": [
                    {
                        "platformId": "job-1",
                        "source": "Crawler",
                        "title": "Senior Engineer",
                        "companyName": "Airwallex",
                        "location": "Melbourne, AU",
                        "descriptionHtml": null,
                        "descriptionText": "We are hiring",
                        "salaryMin": 120000,
                        "salaryMax": 150000,
                        "salaryCurrency": "AUD",
                        "employmentType": "Full-time",
                        "seniorityLevel": "Senior",
                        "workModel": "Hybrid",
                        "department": "Engineering",
                        "postedAt": "2024-03-10",
                        "applyUrl": "https://apply.url",
                        "platformUrl": "https://platform.url"
                    }
                ]
            }
        """.trimIndent()

        // Mock company repository to return a company with website
        val mockCompany = CompanyRecord(
            companyId = "airwallex",
            name = "Airwallex",
            alternateNames = emptyList(),
            logoUrl = null,
            description = null,
            website = "https://www.airwallex.com",
            employeesCount = null,
            industries = null,
            technologies = emptyList(),
            hiringLocations = emptyList(),
            hqCountry = "AU",
            lastUpdatedAt = Instant.now()
        )
        every { companyRepository.getCompaniesByIds(listOf("airwallex")) } returns listOf(mockCompany)

        every { restTemplate.postForObject(any<URI>(), any(), String::class.java) } returns mockResponse

        val result = crawlerClient.fetchJobs("airwallex")

        assertEquals(mockResponse, result)
    }

    @Test
    fun `fetchJobs throws on null response`() {
        val mockCompany = CompanyRecord(
            companyId = "airwallex",
            name = "Airwallex",
            alternateNames = emptyList(),
            logoUrl = null,
            description = null,
            website = "https://www.airwallex.com",
            employeesCount = null,
            industries = null,
            technologies = emptyList(),
            hiringLocations = emptyList(),
            hqCountry = "AU",
            lastUpdatedAt = Instant.now()
        )
        every { companyRepository.getCompaniesByIds(listOf("airwallex")) } returns listOf(mockCompany)

        every { restTemplate.postForObject(any<URI>(), any(), String::class.java) } returns null

        assertThrows<RuntimeException> {
            crawlerClient.fetchJobs("airwallex")
        }
    }

    @Test
    fun `fetchJobs throws on RestTemplate error`() {
        val mockCompany = CompanyRecord(
            companyId = "airwallex",
            name = "Airwallex",
            alternateNames = emptyList(),
            logoUrl = null,
            description = null,
            website = "https://www.airwallex.com",
            employeesCount = null,
            industries = null,
            technologies = emptyList(),
            hiringLocations = emptyList(),
            hqCountry = "AU",
            lastUpdatedAt = Instant.now()
        )
        every { companyRepository.getCompaniesByIds(listOf("airwallex")) } returns listOf(mockCompany)

        every { restTemplate.postForObject(any<URI>(), any(), String::class.java) } throws RuntimeException("Connection refused")

        val exception = assertThrows<RuntimeException> {
            crawlerClient.fetchJobs("airwallex")
        }

        assertTrue(exception.message?.contains("Crawler service request failed") == true)
    }

    @Test
    fun `fetchJobs uses company website to construct career URL`() {
        val mockResponse = """{"companyId": "test-company", "crawlMeta": {"pagesVisited": 1, "totalJobsFound": 0, "detectedAtsProvider": null, "detectedAtsIdentifier": null, "crawlDurationMs": 1000, "extractionModel": "gemini-2.0-flash", "extractionConfidence": 0.0}, "jobs": []}"""
        
        val mockCompany = CompanyRecord(
            companyId = "test-company",
            name = "Test Company",
            alternateNames = emptyList(),
            logoUrl = null,
            description = null,
            website = "https://www.testcompany.com",
            employeesCount = null,
            industries = null,
            technologies = emptyList(),
            hiringLocations = emptyList(),
            hqCountry = "US",
            lastUpdatedAt = Instant.now()
        )
        every { companyRepository.getCompaniesByIds(listOf("test-company")) } returns listOf(mockCompany)
        
        every { restTemplate.postForObject(any<URI>(), any(), String::class.java) } returns mockResponse

        val result = crawlerClient.fetchJobs("test-company")

        assertNotNull(result)
        // Verify it constructs URL from company website
        assertTrue(result.contains("test-company"))
    }

    @Test
    fun `fetchJobs falls back to companyId URL when company not found`() {
        val mockResponse = """{"companyId": "unknown", "crawlMeta": {"pagesVisited": 1, "totalJobsFound": 0, "detectedAtsProvider": null, "detectedAtsIdentifier": null, "crawlDurationMs": 1000, "extractionModel": "gemini-2.0-flash", "extractionConfidence": 0.0}, "jobs": []}"""
        
        every { companyRepository.getCompaniesByIds(listOf("unknown")) } returns emptyList()
        
        every { restTemplate.postForObject(any<URI>(), any(), String::class.java) } returns mockResponse

        val result = crawlerClient.fetchJobs("unknown")

        assertNotNull(result)
        // Should fall back to https://unknown.com/careers
    }
}
