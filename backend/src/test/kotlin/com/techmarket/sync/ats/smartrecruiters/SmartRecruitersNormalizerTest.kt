package com.techmarket.sync.ats.smartrecruiters

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SmartRecruitersNormalizerTest {

    private val normalizer = SmartRecruitersNormalizer()
    private val mapper     = jacksonObjectMapper()

    // ── Unit tests ─────────────────────────────────────────────────────────────

    @Test
    fun `should normalize a standard job posting`() {
        val json = """
            {
              "totalFound": 1,
              "content": [
                {
                  "id": "sr-abc-123",
                  "name": "Senior Software Engineer",
                  "uuid": "aaaa-bbbb-cccc",
                  "company": { "identifier": "acme", "name": "Acme Corp" },
                  "releasedDate": "2026-02-01T00:00:00.000+0000",
                  "location": {
                    "city": "Sydney",
                    "region": "NSW",
                    "country": "AU",
                    "remote": false,
                    "hybrid": false,
                    "fullLocation": "Sydney, NSW, AU"
                  },
                  "department": { "id": "dept-eng", "label": "Engineering" },
                  "typeOfEmployment": { "id": "permanent", "label": "Permanent" },
                  "experienceLevel": { "id": "mid_senior_level", "label": "Mid-Senior level" },
                  "customField": []
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(1, normalized.size)
        val job = normalized[0]
        assertEquals("sr-abc-123", job.platformId)
        assertEquals("Senior Software Engineer", job.title)
        assertEquals("Sydney, NSW, AU", job.location)
        assertEquals("Engineering", job.department)
        assertEquals("Full-time", job.employmentType)
        assertEquals("Mid-Senior", job.seniorityLevel)
        assertNull(job.workModel)
        assertEquals("2026-02-01T00:00:00.000+0000", job.postedAt)
        assertEquals("https://careers.smartrecruiters.com/acme/sr-abc-123", job.applyUrl)
        assertEquals("SmartRecruiters", job.source)
    }

    @Test
    fun `should set workModel to Remote when remote flag is true`() {
        val json = """
            {
              "content": [
                {
                  "id": "remote-job",
                  "name": "Remote Engineer",
                  "company": { "identifier": "acme", "name": "Acme" },
                  "releasedDate": "2026-01-01T00:00:00.000+0000",
                  "location": { "city": "Sydney", "remote": true, "hybrid": false },
                  "typeOfEmployment": { "id": "permanent", "label": "Permanent" },
                  "experienceLevel": { "id": "entry_level", "label": "Entry level" },
                  "customField": []
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Remote", normalized[0].workModel)
        assertEquals("Entry-level", normalized[0].seniorityLevel)
    }

    @Test
    fun `should set workModel to Hybrid when hybrid flag is true`() {
        val json = """
            {
              "content": [
                {
                  "id": "hybrid-job",
                  "name": "Hybrid PM",
                  "company": { "identifier": "acme", "name": "Acme" },
                  "releasedDate": "2026-01-01T00:00:00.000+0000",
                  "location": { "city": "Melbourne", "remote": false, "hybrid": true },
                  "typeOfEmployment": { "id": "permanent", "label": "Permanent" },
                  "experienceLevel": { "id": "director", "label": "Director" },
                  "customField": []
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Hybrid", normalized[0].workModel)
        assertEquals("Director", normalized[0].seniorityLevel)
    }

    @Test
    fun `should build location from parts when fullLocation is absent`() {
        val json = """
            {
              "content": [
                {
                  "id": "loc-job",
                  "name": "Engineer",
                  "company": { "identifier": "acme", "name": "Acme" },
                  "releasedDate": "2026-01-01T00:00:00.000+0000",
                  "location": { "city": "Brisbane", "region": "QLD", "country": "AU", "remote": false, "hybrid": false },
                  "typeOfEmployment": { "id": "permanent", "label": "Permanent" },
                  "experienceLevel": { "id": "senior_level", "label": "Senior level" },
                  "customField": []
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Brisbane, QLD, AU", normalized[0].location)
    }

    @Test
    fun `should handle missing content array`() {
        val normalized = normalizer.normalize(mapper.readTree("{}"))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should handle empty content array`() {
        val normalized = normalizer.normalize(mapper.readTree("""{"totalFound":0,"content":[]}"""))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should skip jobs missing id and continue`() {
        val json = """
            {
              "content": [
                { "name": "No ID" },
                {
                  "id": "valid-1",
                  "name": "Valid Job",
                  "company": { "identifier": "acme", "name": "Acme" },
                  "releasedDate": "2026-01-01T00:00:00.000+0000",
                  "location": { "fullLocation": "Sydney, AU", "remote": false, "hybrid": false },
                  "typeOfEmployment": { "id": "permanent", "label": "Permanent" },
                  "experienceLevel": { "id": "entry_level", "label": "Entry level" },
                  "customField": []
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))
        assertEquals(1, normalized.size)
        assertEquals("valid-1", normalized[0].platformId)
    }

    // ── Contract test (fixture-based) ─────────────────────────────────────────

    @Test
    fun `contract - should normalize real carsales fixture`() {
        val fixture = javaClass.getResourceAsStream("/fixtures/smartrecruiters_carsales.json")!!
            .bufferedReader().readText()

        val normalized = normalizer.normalize(mapper.readTree(fixture))

        assertEquals(3, normalized.size)

        val first = normalized[0]
        assertEquals("sr-job-001", first.platformId)
        assertEquals("Senior Software Engineer", first.title)
        assertEquals("Melbourne, VIC, AU", first.location)
        assertEquals("Engineering", first.department)
        assertEquals("Full-time", first.employmentType)
        assertEquals("Mid-Senior", first.seniorityLevel)
        assertEquals("Hybrid", first.workModel)
        assertEquals("2026-01-20T00:00:00.000+0000", first.postedAt)
        assertEquals("https://careers.smartrecruiters.com/carsales/sr-job-001", first.applyUrl)

        val second = normalized[1]
        assertEquals("Remote", second.workModel)
        assertEquals("Entry-level", second.seniorityLevel)
    }
}
