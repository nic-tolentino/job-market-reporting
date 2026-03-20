package com.techmarket.sync.ats.workable

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class WorkableNormalizerTest {

    private val normalizer = WorkableNormalizer()
    private val mapper     = jacksonObjectMapper()

    // ── Unit tests ─────────────────────────────────────────────────────────────

    @Test
    fun `should normalize a standard job posting`() {
        val json = """
            {
              "total": 1,
              "results": [
                {
                  "id": "wl-abc-001",
                  "shortcode": "XYZ987",
                  "title": "Software Engineer",
                  "remote": false,
                  "location": { "country": "Australia", "countryCode": "AU", "city": "Sydney", "region": "New South Wales" },
                  "locations": [],
                  "state": "published",
                  "type": "PF",
                  "department": ["Engineering"],
                  "workplace": "onsite"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals(1, normalized.size)
        val job = normalized[0]
        assertEquals("wl-abc-001", job.platformId)
        assertEquals("Software Engineer", job.title)
        assertEquals("Sydney, New South Wales, Australia", job.location)
        assertEquals("Engineering", job.department)
        assertEquals("Full-time", job.employmentType)
        assertEquals("On-site", job.workModel)
        assertEquals("https://apply.workable.com/j/XYZ987", job.applyUrl)
        assertEquals("Workable", job.source)
    }

    @Test
    fun `should set workModel to Remote from workplace field`() {
        val json = """
            {
              "results": [
                {
                  "id": "wl-remote",
                  "shortcode": "REM001",
                  "title": "Remote Engineer",
                  "remote": true,
                  "location": { "country": "Australia", "countryCode": "AU", "city": "", "region": "" },
                  "locations": [],
                  "type": "PF",
                  "department": [],
                  "workplace": "remote"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Remote", normalized[0].workModel)
    }

    @Test
    fun `should set workModel to Remote from remote flag when workplace absent`() {
        val json = """
            {
              "results": [
                {
                  "id": "wl-flag-remote",
                  "shortcode": "FLAG01",
                  "title": "Remote Role",
                  "remote": true,
                  "location": { "country": "Australia", "countryCode": "AU", "city": "", "region": "" },
                  "locations": [],
                  "type": "PF",
                  "department": []
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Remote", normalized[0].workModel)
    }

    @Test
    fun `should set workModel to Hybrid`() {
        val json = """
            {
              "results": [
                {
                  "id": "wl-hybrid",
                  "shortcode": "HYB001",
                  "title": "Hybrid Role",
                  "remote": false,
                  "location": { "country": "Australia", "countryCode": "AU", "city": "Melbourne", "region": "Victoria" },
                  "locations": [],
                  "type": "PF",
                  "department": [],
                  "workplace": "hybrid"
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Hybrid", normalized[0].workModel)
    }

    @Test
    fun `should normalize employment type codes`() {
        val cases = mapOf(
            "PF"  to "Full-time",
            "PP"  to "Part-time",
            "PFL" to "Freelance",
            "PIT" to "Internship",
            "PCT" to "Contract",
            "PT"  to "Temporary"
        )
        cases.forEach { (code, expected) ->
            val json = """{"results":[{"id":"t","shortcode":"T","title":"T","remote":false,"location":{},"locations":[],"type":"$code","department":[]}]}"""
            val normalized = normalizer.normalize(mapper.readTree(json))
            assertEquals(expected, normalized[0].employmentType, "Failed for type code $code")
        }
    }

    @Test
    fun `should join multiple departments`() {
        val json = """
            {
              "results": [
                {
                  "id": "dept-job",
                  "shortcode": "DEPT1",
                  "title": "Cross-team Role",
                  "remote": false,
                  "location": {},
                  "locations": [],
                  "type": "PF",
                  "department": ["Engineering", "Product"]
                }
              ]
            }
        """.trimIndent()

        val normalized = normalizer.normalize(mapper.readTree(json))

        assertEquals("Engineering, Product", normalized[0].department)
    }

    @Test
    fun `should handle missing results array`() {
        val normalized = normalizer.normalize(mapper.readTree("{}"))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should handle empty results array`() {
        val normalized = normalizer.normalize(mapper.readTree("""{"total":0,"results":[]}"""))
        assertTrue(normalized.isEmpty())
    }

    @Test
    fun `should skip jobs missing id and continue`() {
        val json = """
            {
              "results": [
                { "title": "No ID" },
                {
                  "id": "valid-1",
                  "shortcode": "V001",
                  "title": "Valid",
                  "remote": false,
                  "location": {},
                  "locations": [],
                  "type": "PF",
                  "department": []
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
    fun `contract - should normalize real mathspace fixture`() {
        val fixture = javaClass.getResourceAsStream("/fixtures/workable_mathspace.json")!!
            .bufferedReader().readText()

        val normalized = normalizer.normalize(mapper.readTree(fixture))

        assertEquals(3, normalized.size)

        val first = normalized[0]
        assertEquals("wl-job-001", first.platformId)
        assertEquals("Software Engineer", first.title)
        assertEquals("Sydney, New South Wales, Australia", first.location)
        assertEquals("Engineering", first.department)
        assertEquals("Full-time", first.employmentType)
        assertEquals("On-site", first.workModel)
        assertEquals("https://apply.workable.com/j/ABC123", first.applyUrl)

        val second = normalized[1]
        assertEquals("Remote", second.workModel)

        val third = normalized[2]
        assertEquals("Part-time", third.employmentType)
        assertEquals("Hybrid", third.workModel)
    }
}
