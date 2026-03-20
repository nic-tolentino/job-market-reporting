package com.techmarket.sync.ats.lever

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LeverNormalizerTest {

    private val normalizer = LeverNormalizer()
    private val mapper = jacksonObjectMapper()

    @Test
    fun `should normalize a standard lever job array`() {
        val json = """
            [
              {
                "id": "abc-123",
                "text": "Senior Software Engineer",
                "categories": {
                  "location": "San Francisco, CA",
                  "department": "Engineering",
                  "commitment": "Full-time"
                },
                "content": {
                  "descriptionHtml": "<p>We are hiring.</p>"
                },
                "createdAt": 1700000000000,
                "updatedAt": 1710000000000,
                "hostedUrl": "https://jobs.lever.co/acme/abc-123",
                "applyUrl": "https://jobs.lever.co/acme/abc-123/apply"
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(1, normalized.size)
        val job = normalized[0]
        assertEquals("abc-123", job.platformId)
        assertEquals("Senior Software Engineer", job.title)
        assertEquals("San Francisco, CA", job.location)
        assertEquals("Engineering", job.department)
        assertEquals("Full-time", job.employmentType)
        assertEquals("<p>We are hiring.</p>", job.descriptionHtml)
        assertEquals("We are hiring.", job.descriptionText)
        assertEquals("https://jobs.lever.co/acme/abc-123/apply", job.applyUrl)
        assertEquals("https://jobs.lever.co/acme/abc-123", job.platformUrl)
        assertEquals("Lever", job.source)
        assertNotNull(job.postedAt)
        assertNotNull(job.firstPublishedAt)
    }

    @Test
    fun `should normalise salary fields when present`() {
        val json = """
            [
              {
                "id": "sal-1",
                "text": "Staff Engineer",
                "categories": {},
                "content": {},
                "createdAt": 1700000000000,
                "salaryRange": { "min": 120000, "max": 180000, "currency": "USD", "interval": "per-year" }
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(1, normalized.size)
        val job = normalized[0]
        assertEquals(120000, job.salaryMin)
        assertEquals(180000, job.salaryMax)
        assertEquals("USD", job.salaryCurrency)
    }

    @Test
    fun `should fall back to team when department is absent`() {
        val json = """
            [
              {
                "id": "team-1",
                "text": "Designer",
                "categories": { "team": "Design" },
                "content": {},
                "createdAt": 1700000000000
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Design", normalized[0].department)
    }

    @Test
    fun `should use hostedUrl as applyUrl when applyUrl absent`() {
        val json = """
            [
              {
                "id": "hosted-1",
                "text": "Engineer",
                "categories": {},
                "content": {},
                "createdAt": 1700000000000,
                "hostedUrl": "https://jobs.lever.co/acme/hosted-1"
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("https://jobs.lever.co/acme/hosted-1", normalized[0].applyUrl)
    }

    @Test
    fun `should handle empty array`() {
        val normalized = normalizer.normalize(mapper.readTree("[]"))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should handle postings-wrapped response shape`() {
        val json = """{"postings": [{"id": "wrap-1", "text": "Dev", "categories": {}, "content": {}, "createdAt": 1700000000000}]}"""
        val normalized = normalizer.normalize(mapper.readTree(json))
        assertEquals(1, normalized.size)
        assertEquals("wrap-1", normalized[0].platformId)
    }

    @Test
    fun `should skip jobs missing id and continue`() {
        val json = """
            [
              { "text": "No ID" },
              { "id": "valid-1", "text": "Valid", "categories": {}, "content": {}, "createdAt": 1700000000000 }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))
        assertEquals(1, normalized.size)
        assertEquals("valid-1", normalized[0].platformId)
    }

    @Test
    fun `should return empty list for unrecognised response shape`() {
        val normalized = normalizer.normalize(mapper.readTree("""{"jobs": []}"""))
        assertTrue(normalized.isEmpty())
    }
}
