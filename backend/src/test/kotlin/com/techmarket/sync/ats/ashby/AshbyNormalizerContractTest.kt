package com.techmarket.sync.ats.ashby

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Contract test for [AshbyNormalizer] using a representative real-world API
 * response fixture (v2 format). This test guards against Ashby API schema changes.
 */
class AshbyNormalizerContractTest {

    private val normalizer = AshbyNormalizer()
    private val mapper     = jacksonObjectMapper()

    @Test
    fun `contract - should normalize relevanceai v2 fixture with expected field mapping`() {
        val fixture = javaClass.getResourceAsStream("/fixtures/ashby_relevanceai.json")!!
            .bufferedReader().readText()

        val normalized = normalizer.normalize(mapper.readTree(fixture))

        assertEquals(2, normalized.size)

        val first = normalized[0]
        assertEquals("c1d2e3f4-a5b6-7890-cdef-012345678901", first.platformId)
        assertEquals("Senior Full Stack Engineer", first.title)
        // Primary + secondaryLocations joined
        assertEquals("Sydney, NSW, Australia, Remote, Australia", first.location)
        assertEquals("Engineering", first.department)
        assertEquals("Full-time", first.employmentType)
        assertEquals("Hybrid", first.workModel)
        assertEquals("2026-02-01T09:00:00.000Z", first.postedAt)
        assertEquals("https://jobs.ashbyhq.com/relevanceai/c1d2e3f4-a5b6-7890-cdef-012345678901/application", first.applyUrl)
        assertEquals("https://jobs.ashbyhq.com/relevanceai/c1d2e3f4-a5b6-7890-cdef-012345678901", first.platformUrl)
        assertEquals("Ashby", first.source)
        assertEquals(150000, first.salaryMin)
        assertEquals(220000, first.salaryMax)
        assertEquals("AUD", first.salaryCurrency)
        assertNotNull(first.descriptionHtml)
        assertNotNull(first.descriptionText)

        val second = normalized[1]
        assertEquals("d2e3f4a5-b6c7-8901-defa-123456789012", second.platformId)
        assertEquals("Remote", second.workModel)
        assertEquals("Research", second.department)
        assertNull(second.salaryMin)
    }
}
