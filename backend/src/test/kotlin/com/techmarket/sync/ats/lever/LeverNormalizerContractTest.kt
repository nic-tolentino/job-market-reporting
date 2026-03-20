package com.techmarket.sync.ats.lever

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Contract test for [LeverNormalizer] using a representative real-world API
 * response fixture. This test guards against Lever API schema changes.
 */
class LeverNormalizerContractTest {

    private val normalizer = LeverNormalizer()
    private val mapper     = jacksonObjectMapper()

    @Test
    fun `contract - should normalize spotify fixture with expected field mapping`() {
        val fixture = javaClass.getResourceAsStream("/fixtures/lever_spotify.json")!!
            .bufferedReader().readText()

        val normalized = normalizer.normalize(mapper.readTree(fixture))

        assertEquals(2, normalized.size)

        val first = normalized[0]
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", first.platformId)
        assertEquals("Backend Engineer - Personalization", first.title)
        assertEquals("Stockholm, Sweden", first.location)
        assertEquals("Engineering", first.department)
        assertEquals("Full-time", first.employmentType)
        assertEquals("https://jobs.lever.co/spotify/a1b2c3d4-e5f6-7890-abcd-ef1234567890/apply", first.applyUrl)
        assertEquals("https://jobs.lever.co/spotify/a1b2c3d4-e5f6-7890-abcd-ef1234567890", first.platformUrl)
        assertEquals("Lever", first.source)
        assertNotNull(first.descriptionHtml)
        assertNotNull(first.firstPublishedAt) // epoch 1705276800000 → ISO
        assertNotNull(first.postedAt)         // epoch 1707955200000 → ISO
        assertEquals(120000, first.salaryMin)
        assertEquals(180000, first.salaryMax)
        assertEquals("SEK", first.salaryCurrency)

        val second = normalized[1]
        assertEquals("b2c3d4e5-f6a7-8901-bcde-f12345678901", second.platformId)
        // No department — falls back to team
        assertEquals("Consumer iOS", second.department)
        assertNull(second.salaryMin)
    }
}
