package com.techmarket.sync.ats.greenhouse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Contract test for [GreenhouseNormalizer] using a representative real-world API
 * response fixture. This test guards against Greenhouse API schema changes.
 */
class GreenhouseNormalizerContractTest {

    private val normalizer = GreenhouseNormalizer()
    private val mapper     = jacksonObjectMapper()

    @Test
    fun `contract - should normalize stripe fixture with expected field mapping`() {
        val fixture = javaClass.getResourceAsStream("/fixtures/greenhouse_stripe.json")!!
            .bufferedReader().readText()

        val normalized = normalizer.normalize(mapper.readTree(fixture))

        assertEquals(2, normalized.size)

        val first = normalized[0]
        assertEquals("6530908002", first.platformId)
        assertEquals("Software Engineer, Payments", first.title)
        // Office location takes priority over board location
        assertEquals("San Francisco, CA, United States", first.location)
        assertEquals("Engineering", first.department)
        assertEquals("2026-02-10T17:23:10-05:00", first.postedAt)
        assertEquals("2026-01-15T09:00:00-05:00", first.firstPublishedAt)
        assertEquals("https://stripe.com/jobs/listing/software-engineer-payments/6530908002", first.applyUrl)
        assertEquals("Greenhouse", first.source)
        assertNotNull(first.descriptionHtml)
        assertNotNull(first.descriptionText)

        val second = normalized[1]
        assertEquals("6530909003", second.platformId)
        // No office — falls back to location.name
        assertEquals("Remote", second.location)
        assertEquals("Data", second.department)
    }
}
