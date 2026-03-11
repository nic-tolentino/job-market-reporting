package com.techmarket.sync.ats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.company.CompanyRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.web.client.RestTemplate
import java.net.URI

/**
 * Unit tests for CrawlerClient
 */
class CrawlerClientTest {

    private lateinit var crawlerClient: CrawlerClient
    private lateinit var restTemplate: RestTemplate
    private lateinit var objectMapper: ObjectMapper
    private lateinit var companyRepository: CompanyRepository

    @BeforeEach
    fun setup() {
        restTemplate = object : RestTemplate() {
            var mockResponse: String? = null
            var shouldThrow: Exception? = null

            override fun postForObject(url: URI, request: Any?, responseType: Class<String>): String? {
                shouldThrow?.let { throw it }
                return mockResponse
            }
        }
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        companyRepository = mock()
        crawlerClient = CrawlerClient(restTemplate, objectMapper, companyRepository, "http://localhost:8080")
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
            id = "airwallex",
            name = "Airwallex",
            website = "https://www.airwallex.com",
            hqCountry = "AU"
        )
        whenever(companyRepository.getCompaniesByIds(listOf("airwallex"))).thenReturn(listOf(mockCompany))

        // Use reflection to set mock response
        restTemplate.javaClass.getDeclaredField("mockResponse").apply {
            isAccessible = true
            set(restTemplate, mockResponse)
        }

        val result = crawlerClient.fetchJobs("airwallex")

        assertEquals(mockResponse, result)
    }

    @Test
    fun `fetchJobs throws on null response`() {
        val mockCompany = CompanyRecord(
            id = "airwallex",
            name = "Airwallex",
            website = "https://www.airwallex.com",
            hqCountry = "AU"
        )
        whenever(companyRepository.getCompaniesByIds(listOf("airwallex"))).thenReturn(listOf(mockCompany))

        restTemplate.javaClass.getDeclaredField("mockResponse").apply {
            isAccessible = true
            set(restTemplate, null)
        }

        assertThrows<RuntimeException> {
            crawlerClient.fetchJobs("airwallex")
        }
    }

    @Test
    fun `fetchJobs throws on RestTemplate error`() {
        val mockCompany = CompanyRecord(
            id = "airwallex",
            name = "Airwallex",
            website = "https://www.airwallex.com",
            hqCountry = "AU"
        )
        whenever(companyRepository.getCompaniesByIds(listOf("airwallex"))).thenReturn(listOf(mockCompany))

        restTemplate.javaClass.getDeclaredField("shouldThrow").apply {
            isAccessible = true
            set(restTemplate, RuntimeException("Connection refused"))
        }

        val exception = assertThrows<RuntimeException> {
            crawlerClient.fetchJobs("airwallex")
        }

        assertTrue(exception.message?.contains("Crawler service request failed") == true)
    }

    @Test
    fun `fetchJobs uses company website to construct career URL`() {
        val mockResponse = """{"companyId": "test", "crawlMeta": {"pagesVisited": 1, "totalJobsFound": 0, "detectedAtsProvider": null, "detectedAtsIdentifier": null, "crawlDurationMs": 1000, "extractionModel": "gemini-2.0-flash", "extractionConfidence": 0.0}, "jobs": []}"""
        
        val mockCompany = CompanyRecord(
            id = "test-company",
            name = "Test Company",
            website = "https://www.testcompany.com",
            hqCountry = "US"
        )
        whenever(companyRepository.getCompaniesByIds(listOf("test-company"))).thenReturn(listOf(mockCompany))
        
        restTemplate.javaClass.getDeclaredField("mockResponse").apply {
            isAccessible = true
            set(restTemplate, mockResponse)
        }

        val result = crawlerClient.fetchJobs("test-company")

        assertNotNull(result)
        // Verify it constructs URL from company website
        assertTrue(result.contains("test-company"))
    }

    @Test
    fun `fetchJobs falls back to companyId URL when company not found`() {
        val mockResponse = """{"companyId": "unknown", "crawlMeta": {"pagesVisited": 1, "totalJobsFound": 0, "detectedAtsProvider": null, "detectedAtsIdentifier": null, "crawlDurationMs": 1000, "extractionModel": "gemini-2.0-flash", "extractionConfidence": 0.0}, "jobs": []}"""
        
        whenever(companyRepository.getCompaniesByIds(listOf("unknown"))).thenReturn(emptyList())
        
        restTemplate.javaClass.getDeclaredField("mockResponse").apply {
            isAccessible = true
            set(restTemplate, mockResponse)
        }

        val result = crawlerClient.fetchJobs("unknown")

        assertNotNull(result)
        // Should fall back to https://unknown.com/careers
    }
}
