package com.techmarket.sync.ats.ashby

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AshbyNormalizerTest {

    private val normalizer = AshbyNormalizer()
    private val mapper = jacksonObjectMapper()

    // ── v2 API format (current) ────────────────────────────────────────────────

    @Test
    fun `v2 - should normalize a standard job posting`() {
        val json = """
            {
              "apiVersion": "2025-01",
              "organization": { "name": "Acme Corp" },
              "jobs": [
                {
                  "id": "job-abc-123",
                  "title": "Senior Backend Engineer",
                  "team": "Engineering",
                  "location": "Sydney, NSW",
                  "secondaryLocations": [],
                  "employmentType": "FullTime",
                  "isRemote": false,
                  "workplaceType": "OnSite",
                  "publishedAt": "2026-03-10T10:00:00.000Z",
                  "jobUrl": "https://jobs.ashbyhq.com/acme/job-abc-123",
                  "applyUrl": "https://jobs.ashbyhq.com/acme/job-abc-123/application",
                  "descriptionHtml": "<p>We are looking for a Senior Backend Engineer.</p>",
                  "descriptionPlain": "We are looking for a Senior Backend Engineer."
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(1, normalized.size)
        val job = normalized[0]
        assertEquals("job-abc-123", job.platformId)
        assertEquals("Senior Backend Engineer", job.title)
        assertEquals("Sydney, NSW", job.location)
        assertEquals("Engineering", job.department)
        assertEquals("Full-time", job.employmentType)
        assertEquals("On-site", job.workModel)
        assertEquals("<p>We are looking for a Senior Backend Engineer.</p>", job.descriptionHtml)
        assertEquals("We are looking for a Senior Backend Engineer.", job.descriptionText)
        assertEquals("https://jobs.ashbyhq.com/acme/job-abc-123/application", job.applyUrl)
        assertEquals("https://jobs.ashbyhq.com/acme/job-abc-123", job.platformUrl)
        assertEquals("2026-03-10T10:00:00.000Z", job.postedAt)
        assertEquals("2026-03-10T10:00:00.000Z", job.firstPublishedAt)
        assertEquals("Ashby", job.source)
    }

    @Test
    fun `v2 - should set workModel to Remote from workplaceType`() {
        val json = """
            {
              "jobs": [
                {
                  "id": "remote-job",
                  "title": "Remote Engineer",
                  "workplaceType": "Remote",
                  "isRemote": false,
                  "publishedAt": "2026-03-10T10:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Remote", normalized[0].workModel)
    }

    @Test
    fun `v2 - should set workModel to Hybrid from workplaceType`() {
        val json = """
            {
              "jobs": [
                {
                  "id": "hybrid-job",
                  "title": "Hybrid Engineer",
                  "workplaceType": "Hybrid",
                  "publishedAt": "2026-03-10T10:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Hybrid", normalized[0].workModel)
    }

    @Test
    fun `v2 - should join primary location with secondaryLocations`() {
        val json = """
            {
              "jobs": [
                {
                  "id": "multi-loc",
                  "title": "Engineer",
                  "location": "Sydney, NSW",
                  "secondaryLocations": ["Melbourne, VIC", "Remote"],
                  "publishedAt": "2026-03-10T10:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Sydney, NSW, Melbourne, VIC, Remote", normalized[0].location)
    }

    @Test
    fun `v2 - should handle missing jobs array`() {
        val normalized = normalizer.normalize(mapper.readTree("{}"))
        assertTrue(normalized.isEmpty())
    }

    // ── v1 API format (legacy backward-compat) ────────────────────────────────

    @Test
    fun `v1 backward-compat - should normalize using jobPostings key`() {
        val json = """
            {
              "organization": { "name": "Acme Corp" },
              "jobPostings": [
                {
                  "id": "job-abc-123",
                  "title": "Senior Backend Engineer",
                  "teamName": "Engineering",
                  "locationName": "Sydney, NSW",
                  "allLocations": ["Sydney, NSW"],
                  "employmentType": "FullTime",
                  "isRemote": false,
                  "publishedAt": "2026-03-10T10:00:00.000Z",
                  "jobUrl": "https://jobs.ashbyhq.com/acme/job-abc-123",
                  "applyUrl": "https://jobs.ashbyhq.com/acme/job-abc-123/application",
                  "descriptionHtml": "<p>We are looking for a Senior Backend Engineer.</p>",
                  "descriptionPlain": "We are looking for a Senior Backend Engineer."
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(1, normalized.size)
        val job = normalized[0]
        assertEquals("job-abc-123", job.platformId)
        assertEquals("Engineering", job.department)
        assertEquals("Sydney, NSW", job.location)
        assertEquals("Full-time", job.employmentType)
    }

    @Test
    fun `v1 backward-compat - should set workModel to Remote from isRemote`() {
        val json = """
            {
              "jobPostings": [
                {
                  "id": "remote-job",
                  "title": "Remote Engineer",
                  "isRemote": true,
                  "publishedAt": "2026-03-10T10:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Remote", normalized[0].workModel)
    }

    @Test
    fun `v1 backward-compat - should join multiple allLocations`() {
        val json = """
            {
              "jobPostings": [
                {
                  "id": "multi-loc",
                  "title": "Engineer",
                  "allLocations": ["Sydney, NSW", "Melbourne, VIC", "Remote"],
                  "publishedAt": "2026-03-10T10:00:00.000Z"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Sydney, NSW, Melbourne, VIC, Remote", normalized[0].location)
    }

    // ── Shared behaviour ──────────────────────────────────────────────────────

    @Test
    fun `should normalize employment types correctly`() {
        val cases = mapOf(
            "FullTime"   to "Full-time",
            "PartTime"   to "Part-time",
            "Contract"   to "Contract",
            "Internship" to "Internship",
            "Temporary"  to "Temporary"
        )
        cases.forEach { (raw, expected) ->
            val json = """{"jobs":[{"id":"$raw","title":"T","employmentType":"$raw","publishedAt":"2026-01-01T00:00:00Z"}]}"""
            val normalized = normalizer.normalize(mapper.readTree(json))
            assertEquals(expected, normalized[0].employmentType, "Failed for $raw")
        }
    }

    @Test
    fun `should extract salary from compensation block`() {
        val json = """
            {
              "jobs": [
                {
                  "id": "sal-job",
                  "title": "Staff Engineer",
                  "publishedAt": "2026-03-10T10:00:00.000Z",
                  "compensation": {
                    "currency": "USD",
                    "summaryComponents": [
                      {
                        "format": "USD",
                        "label": "Base Salary",
                        "minValue": 150000.0,
                        "maxValue": 200000.0,
                        "interval": "YEARLY"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(150000, normalized[0].salaryMin)
        assertEquals(200000, normalized[0].salaryMax)
        assertEquals("USD", normalized[0].salaryCurrency)
    }

    @Test
    fun `should use descriptionPlain when available instead of stripping html`() {
        val json = """
            {
              "jobs": [
                {
                  "id": "plain-job",
                  "title": "Engineer",
                  "publishedAt": "2026-03-10T10:00:00.000Z",
                  "descriptionHtml": "<p>Some <b>HTML</b> content.</p>",
                  "descriptionPlain": "Pre-stripped plain text."
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Pre-stripped plain text.", normalized[0].descriptionText)
    }

    @Test
    fun `should handle empty jobs array`() {
        val normalized = normalizer.normalize(mapper.readTree("""{"jobs":[]}"""))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should skip jobs missing id and continue`() {
        val json = """
            {
              "jobs": [
                { "title": "No ID" },
                { "id": "valid-1", "title": "Valid", "publishedAt": "2026-03-10T10:00:00.000Z" }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))
        assertEquals(1, normalized.size)
        assertEquals("valid-1", normalized[0].platformId)
    }
}
