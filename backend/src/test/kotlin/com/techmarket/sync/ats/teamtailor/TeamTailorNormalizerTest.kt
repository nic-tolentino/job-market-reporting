package com.techmarket.sync.ats.teamtailor

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TeamTailorNormalizerTest {

    private val normalizer = TeamTailorNormalizer()
    private val mapper     = jacksonObjectMapper()

    // ── Unit tests ─────────────────────────────────────────────────────────────

    @Test
    fun `should normalize a standard job posting`() {
        val json = """
            [
              {
                "id": 123456,
                "title": "Backend Engineer",
                "apply_url": "https://acme.teamtailor.com/jobs/123456-backend-engineer",
                "human_status": "open",
                "created_at": "2026-01-10T09:00:00.000+11:00",
                "body": "<p>Join our backend team.</p>",
                "tags": ["engineering"],
                "remote_status": "hybrid",
                "locations": [{ "city": "Sydney", "country": "Australia" }],
                "categories": [{ "name": "Engineering" }],
                "department": { "name": "Product & Engineering" }
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(1, normalized.size)
        val job = normalized[0]
        assertEquals("123456", job.platformId)
        assertEquals("Backend Engineer", job.title)
        assertEquals("Sydney, Australia", job.location)
        assertEquals("Product & Engineering", job.department)
        assertEquals("Hybrid", job.workModel)
        assertEquals("<p>Join our backend team.</p>", job.descriptionHtml)
        assertNotNull(job.descriptionText)
        assertEquals("2026-01-10T09:00:00.000+11:00", job.postedAt)
        assertEquals("https://acme.teamtailor.com/jobs/123456-backend-engineer", job.applyUrl)
        assertEquals("TeamTailor", job.source)
    }

    @Test
    fun `should set workModel to Remote`() {
        val json = """
            [
              {
                "id": 111,
                "title": "Remote Role",
                "apply_url": "https://acme.teamtailor.com/jobs/111",
                "created_at": "2026-01-01T09:00:00.000+11:00",
                "remote_status": "fully_remote",
                "locations": [],
                "categories": []
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Remote", normalized[0].workModel)
    }

    @Test
    fun `should join multiple locations with semicolon`() {
        val json = """
            [
              {
                "id": 222,
                "title": "Multi-location Role",
                "apply_url": "https://acme.teamtailor.com/jobs/222",
                "created_at": "2026-01-01T09:00:00.000+11:00",
                "remote_status": "none",
                "locations": [
                  { "city": "Sydney", "country": "Australia" },
                  { "city": "Melbourne", "country": "Australia" }
                ],
                "categories": []
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Sydney, Australia; Melbourne, Australia", normalized[0].location)
    }

    @Test
    fun `should fall back to category name when department absent`() {
        val json = """
            [
              {
                "id": 333,
                "title": "Designer",
                "apply_url": "https://acme.teamtailor.com/jobs/333",
                "created_at": "2026-01-01T09:00:00.000+11:00",
                "remote_status": "none",
                "locations": [],
                "categories": [{ "name": "Design" }]
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Design", normalized[0].department)
    }

    @Test
    fun `should return empty for non-array response`() {
        val normalized = normalizer.normalize(mapper.readTree("{}"))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should handle empty array`() {
        val normalized = normalizer.normalize(mapper.readTree("[]"))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should skip jobs missing id and continue`() {
        val json = """
            [
              { "title": "No ID" },
              {
                "id": 444,
                "title": "Valid",
                "apply_url": "https://acme.teamtailor.com/jobs/444",
                "created_at": "2026-01-01T09:00:00.000+11:00",
                "remote_status": "none",
                "locations": [],
                "categories": []
              }
            ]
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))
        assertEquals(1, normalized.size)
        assertEquals("444", normalized[0].platformId)
    }

    // ── Contract test (fixture-based) ─────────────────────────────────────────

    @Test
    fun `contract - should normalize teamtailor sample fixture`() {
        val fixture = javaClass.getResourceAsStream("/fixtures/teamtailor_sample.json")!!
            .bufferedReader().readText()

        val normalized = normalizer.normalize(mapper.readTree(fixture))

        assertEquals(2, normalized.size)

        val first = normalized[0]
        assertEquals("987654", first.platformId)
        assertEquals("Backend Engineer", first.title)
        assertEquals("Sydney, Australia", first.location)
        assertEquals("Product & Engineering", first.department)
        assertEquals("Hybrid", first.workModel)
        assertEquals("https://eftsure.teamtailor.com/jobs/987654-backend-engineer", first.applyUrl)
        assertNotNull(first.descriptionHtml)
        assertEquals("2026-01-10T09:00:00.000+11:00", first.postedAt)

        val second = normalized[1]
        assertEquals("987655", second.platformId)
        assertEquals("Melbourne, Australia; Sydney, Australia", second.location)
        assertNull(second.workModel)
    }
}
