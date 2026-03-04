package com.techmarket.sync.ats.greenhouse

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GreenhouseNormalizerTest {

  private val normalizer = GreenhouseNormalizer()
  private val mapper = jacksonObjectMapper()

  @Test
  fun `should normalize greenhouse jobs successfully`() {
    val json =
            """
        {
          "jobs": [
            {
              "id": 12345,
              "title": "Software Engineer",
              "location": { "name": "Remote" },
              "content": "<p>We are looking for a Software Engineer...</p>",
              "departments": [ { "name": "Engineering" } ],
              "offices": [ { "name": "Auckland", "location": "Auckland, New Zealand" } ],
              "updated_at": "2023-10-27T16:00:00Z",
              "absolute_url": "https://boards.greenhouse.io/test/jobs/12345"
            }
          ]
        }
        """.trimIndent()

    val root = mapper.readTree(json)
    val normalized = normalizer.normalize(root)

    assertEquals(1, normalized.size)
    val job = normalized[0]
    assertEquals("12345", job.platformId)
    assertEquals("Software Engineer", job.title)
    assertEquals("Auckland, New Zealand", job.location) // officeLocation should win
    assertEquals("<p>We are looking for a Software Engineer...</p>", job.descriptionHtml)
    assertEquals("We are looking for a Software Engineer...", job.descriptionText)
    assertEquals("Engineering", job.department)
    assertEquals("2023-10-27T16:00:00Z", job.postedAt)
    assertEquals("https://boards.greenhouse.io/test/jobs/12345", job.applyUrl)
    assertEquals("Greenhouse", job.source)
  }

  @Test
  fun `should fallback to location-name if offices array is empty`() {
    val json =
            """
        {
          "jobs": [
            {
              "id": 67890,
              "title": "Product Designer",
              "location": { "name": "Remote, Global" },
              "offices": [],
              "updated_at": "2023-10-27T16:00:00Z"
            }
          ]
        }
        """.trimIndent()

    val normalized = normalizer.normalize(mapper.readTree(json))
    assertEquals(1, normalized.size)
    assertEquals("Remote, Global", normalized[0].location)
  }

  @Test
  fun `should capture first_published_at correctly`() {
    val json =
            """
        {
          "jobs": [
            {
              "id": 111,
              "first_published_at": "2023-01-01T09:00:00Z",
              "updated_at": "2023-10-27T16:00:00Z"
            }
          ]
        }
        """.trimIndent()

    val normalized = normalizer.normalize(mapper.readTree(json))
    assertEquals(1, normalized.size)
    assertEquals("2023-01-01T09:00:00Z", normalized[0].firstPublishedAt)
    assertEquals("2023-10-27T16:00:00Z", normalized[0].postedAt)
  }

  @Test
  fun `should handle missing jobs array`() {
    val root = mapper.readTree("{}")
    val normalized = normalizer.normalize(root)
    assertTrue(normalized.isEmpty())
  }

  @Test
  fun `should handle empty jobs array`() {
    val root = mapper.readTree("""{"jobs": []}""")
    val normalized = normalizer.normalize(root)
    assertTrue(normalized.isEmpty())
  }

  @Test
  fun `should skip jobs with missing id field`() {
    val json =
            """
        {
          "jobs": [
            { "title": "No ID Job" },
            { "id": 999, "title": "Valid Job" }
          ]
        }
        """.trimIndent()

    val normalized = normalizer.normalize(mapper.readTree(json))
    assertEquals(1, normalized.size)
    assertEquals("999", normalized[0].platformId)
  }
}
