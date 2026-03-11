package com.techmarket.sync.ats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Unit tests for CrawlerNormalizer
 */
class CrawlerNormalizerTest {

    private lateinit var normalizer: CrawlerNormalizer
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        objectMapper = ObjectMapper().registerModule(KotlinModule.Builder().build())
        normalizer = CrawlerNormalizer(objectMapper)
    }

    @Test
    fun `normalize maps crawler jobs to NormalizedJob`() {
        val rawData = objectMapper.readTree("""
            {
                "companyId": "airwallex",
                "crawlMeta": {
                    "pagesVisited": 3,
                    "totalJobsFound": 1,
                    "detectedAtsProvider": "ASHBY",
                    "detectedAtsIdentifier": "airwallex",
                    "crawlDurationMs": 5000,
                    "extractionModel": "gemini-2.0-flash",
                    "extractionConfidence": 0.92
                },
                "jobs": [
                    {
                        "platformId": "job-123",
                        "source": "Crawler",
                        "title": "Senior Software Engineer",
                        "companyName": "Airwallex",
                        "location": "Melbourne, AU",
                        "descriptionHtml": null,
                        "descriptionText": "We are looking for a senior engineer...",
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
        """)

        val result = normalizer.normalize(rawData)

        assertEquals(1, result.size)
        assertEquals("Senior Software Engineer", result[0].title)
        assertEquals("Crawler", result[0].source)
        assertEquals("Melbourne, AU", result[0].location)
        assertEquals("Engineering", result[0].department)
    }

    @Test
    fun `normalize rejects jobs without title`() {
        val rawData = objectMapper.readTree("""
            {
                "companyId": "test",
                "crawlMeta": {
                    "pagesVisited": 1,
                    "totalJobsFound": 1,
                    "detectedAtsProvider": null,
                    "detectedAtsIdentifier": null,
                    "crawlDurationMs": 1000,
                    "extractionModel": "gemini-2.0-flash",
                    "extractionConfidence": 0.5
                },
                "jobs": [
                    {
                        "platformId": "job-1",
                        "source": "Crawler",
                        "title": "",
                        "companyName": "Test",
                        "location": "Sydney",
                        "descriptionHtml": null,
                        "descriptionText": "Some description",
                        "salaryMin": null,
                        "salaryMax": null,
                        "salaryCurrency": null,
                        "employmentType": null,
                        "seniorityLevel": null,
                        "workModel": null,
                        "department": null,
                        "postedAt": null,
                        "applyUrl": null,
                        "platformUrl": null
                    }
                ]
            }
        """)

        val result = normalizer.normalize(rawData)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `normalize handles multiple jobs`() {
        val rawData = objectMapper.readTree("""
            {
                "companyId": "canva",
                "crawlMeta": {
                    "pagesVisited": 5,
                    "totalJobsFound": 3,
                    "detectedAtsProvider": "GREENHOUSE",
                    "detectedAtsIdentifier": "canva",
                    "crawlDurationMs": 8000,
                    "extractionModel": "gemini-2.0-flash",
                    "extractionConfidence": 0.88
                },
                "jobs": [
                    {
                        "platformId": "job-1",
                        "source": "Crawler",
                        "title": "Frontend Engineer",
                        "companyName": "Canva",
                        "location": "Sydney, AU",
                        "descriptionHtml": null,
                        "descriptionText": "Build beautiful UIs",
                        "salaryMin": 100000,
                        "salaryMax": 130000,
                        "salaryCurrency": "AUD",
                        "employmentType": "Full-time",
                        "seniorityLevel": "Mid",
                        "workModel": "Hybrid",
                        "department": "Engineering",
                        "postedAt": "2024-03-09",
                        "applyUrl": "https://apply.url/1",
                        "platformUrl": "https://platform.url/1"
                    },
                    {
                        "platformId": "job-2",
                        "source": "Crawler",
                        "title": "Backend Engineer",
                        "companyName": "Canva",
                        "location": "Melbourne, AU",
                        "descriptionHtml": null,
                        "descriptionText": "Build scalable APIs",
                        "salaryMin": 110000,
                        "salaryMax": 140000,
                        "salaryCurrency": "AUD",
                        "employmentType": "Full-time",
                        "seniorityLevel": "Senior",
                        "workModel": "Remote",
                        "department": "Engineering",
                        "postedAt": "2024-03-08",
                        "applyUrl": "https://apply.url/2",
                        "platformUrl": "https://platform.url/2"
                    }
                ]
            }
        """)

        val result = normalizer.normalize(rawData)

        assertEquals(2, result.size)
        assertEquals("Frontend Engineer", result[0].title)
        assertEquals("Backend Engineer", result[1].title)
    }

    @Test
    fun `normalize handles null location`() {
        val rawData = objectMapper.readTree("""
            {
                "companyId": "test",
                "crawlMeta": {
                    "pagesVisited": 1,
                    "totalJobsFound": 1,
                    "detectedAtsProvider": null,
                    "detectedAtsIdentifier": null,
                    "crawlDurationMs": 1000,
                    "extractionModel": "gemini-2.0-flash",
                    "extractionConfidence": 0.8
                },
                "jobs": [
                    {
                        "platformId": "job-1",
                        "source": "Crawler",
                        "title": "Remote Developer",
                        "companyName": "Test",
                        "location": null,
                        "descriptionHtml": null,
                        "descriptionText": "Work from anywhere",
                        "salaryMin": null,
                        "salaryMax": null,
                        "salaryCurrency": null,
                        "employmentType": "Full-time",
                        "seniorityLevel": "Mid",
                        "workModel": "Remote",
                        "department": null,
                        "postedAt": null,
                        "applyUrl": "https://apply.url",
                        "platformUrl": null
                    }
                ]
            }
        """)

        val result = normalizer.normalize(rawData)

        assertEquals(1, result.size)
        assertEquals("", result[0].location) // Null location becomes empty string
    }

    @Test
    fun `normalize generates platformId when missing`() {
        val rawData = objectMapper.readTree("""
            {
                "companyId": "test",
                "crawlMeta": {
                    "pagesVisited": 1,
                    "totalJobsFound": 1,
                    "detectedAtsProvider": null,
                    "detectedAtsIdentifier": null,
                    "crawlDurationMs": 1000,
                    "extractionModel": "gemini-2.0-flash",
                    "extractionConfidence": 0.8
                },
                "jobs": [
                    {
                        "platformId": "",
                        "source": "Crawler",
                        "title": "Software Engineer",
                        "companyName": "Test",
                        "location": "Sydney",
                        "descriptionHtml": null,
                        "descriptionText": "Description",
                        "salaryMin": null,
                        "salaryMax": null,
                        "salaryCurrency": null,
                        "employmentType": null,
                        "seniorityLevel": null,
                        "workModel": null,
                        "department": null,
                        "postedAt": "2024-03-10",
                        "applyUrl": null,
                        "platformUrl": null
                    }
                ]
            }
        """)

        val result = normalizer.normalize(rawData)

        assertEquals(1, result.size)
        assertTrue(result[0].platformId.startsWith("crawl-"))
        assertTrue(result[0].platformId.contains("software-engineer"))
    }

    @Test
    fun `normalize throws on invalid JSON`() {
        val rawData = objectMapper.readTree("""{"invalid": "json structure"}""")

        assertThrows<RuntimeException> {
            normalizer.normalize(rawData)
        }
    }

    @Test
    fun `normalize handles descriptionHtml when descriptionText is null`() {
        val rawData = objectMapper.readTree("""
            {
                "companyId": "test",
                "crawlMeta": {
                    "pagesVisited": 1,
                    "totalJobsFound": 1,
                    "detectedAtsProvider": null,
                    "detectedAtsIdentifier": null,
                    "crawlDurationMs": 1000,
                    "extractionModel": "gemini-2.0-flash",
                    "extractionConfidence": 0.8
                },
                "jobs": [
                    {
                        "platformId": "job-1",
                        "source": "Crawler",
                        "title": "Developer",
                        "companyName": "Test",
                        "location": "Sydney",
                        "descriptionHtml": "<p>HTML description</p>",
                        "descriptionText": null,
                        "salaryMin": null,
                        "salaryMax": null,
                        "salaryCurrency": null,
                        "employmentType": "Full-time",
                        "seniorityLevel": "Mid",
                        "workModel": "Hybrid",
                        "department": null,
                        "postedAt": null,
                        "applyUrl": null,
                        "platformUrl": null
                    }
                ]
            }
        """)

        val result = normalizer.normalize(rawData)

        assertEquals(1, result.size)
        assertEquals("<p>HTML description</p>", result[0].descriptionText)
    }
}
