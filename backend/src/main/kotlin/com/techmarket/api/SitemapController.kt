package com.techmarket.api

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Generates a dynamic XML sitemap for search engine optimization (SEO).
 *
 * Includes:
 * - Static pages (home, about, contact, etc.)
 * - Technology detail pages (/tech/{name})
 * - Company profile pages (/company/{id})
 *
 * Sitemap conforms to the sitemaps.org protocol.
 */
@RestController
@RequestMapping("/api")
class SitemapController(private val bigQuery: BigQuery) {

    private val datasetName = System.getenv("BIGQUERY_DATASET") ?: "tech_market_analytics"

    @GetMapping("/sitemap.xml", produces = ["application/xml"])
    fun getSitemap(): String {
        val baseUrl = "https://devassembly.org"
        val lastMod = Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Static pages
        val staticUrls = listOf(
            "",
            "/contact",
            "/transparency",
            "/privacy",
            "/terms"
        ).map { path ->
            val fullPath = if (path.isEmpty()) baseUrl else "$baseUrl$path"
            "<url><loc>$fullPath</loc><lastmod>$lastMod</lastmod><changefreq>${if (path.isEmpty()) "daily" else "monthly"}</changefreq><priority>${if (path.isEmpty()) "1.0" else "0.5"}</priority></url>"
        }

        // Technology pages
        val techUrls = getTechnologyPages()

        // Company pages
        val companyUrls = getCompanyPages()

        val allUrls = (staticUrls + techUrls + companyUrls).joinToString("\n")

        return buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
            append(allUrls)
            appendLine()
            appendLine("</urlset>")
        }
    }

    /**
     * Fetches all unique technology names from active job postings.
     */
    private fun getTechnologyPages(): List<String> {
        val query = """
            SELECT 
                ARRAY_AGG(DISTINCT tech) AS technologies
            FROM (
                SELECT tech
                FROM `$datasetName.jobs`,
                UNNEST(technologies) AS tech
                WHERE status = 'ACTIVE'
            )
        """.trimIndent()

        return try {
            val config = QueryJobConfiguration.newBuilder(query).build()
            val result = bigQuery.query(config)
            val row = result.values.firstOrNull() ?: return emptyList()

            if (row.get("technologies").isNull) {
                emptyList()
            } else {
                row.get("technologies").repeatedValue.map {
                    val tech = it.stringValue
                    val encodedTech = URLEncoder.encode(tech.lowercase().replace(" ", "-"), StandardCharsets.UTF_8.toString())
                    "<url><loc>https://devassembly.org/tech/$encodedTech</loc><lastmod>${Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)}</lastmod><changefreq>weekly</changefreq><priority>0.8</priority></url>"
                }
            }
        } catch (e: Exception) {
            // Log error but return empty list to prevent sitemap failure
            println("Warning: Failed to fetch technology pages for sitemap: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches all company IDs with active job postings.
     */
    private fun getCompanyPages(): List<String> {
        val query = """
            SELECT 
                company_id,
                MAX(last_updated_at) AS last_updated
            FROM `$datasetName.companies`
            WHERE company_id IN (
                SELECT DISTINCT company_id
                FROM `$datasetName.jobs`
                WHERE status = 'ACTIVE'
            )
            GROUP BY company_id
        """.trimIndent()

        return try {
            val config = QueryJobConfiguration.newBuilder(query).build()
            val result = bigQuery.query(config)

            result.values.map { row ->
                val companyId = row.get("company_id").stringValue
                val lastUpdated = if (row.get("last_updated").isNull) {
                    Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
                } else {
                    parseTimestamp(row.get("last_updated").stringValue)
                }
                "<url><loc>https://devassembly.org/company/$companyId</loc><lastmod>$lastUpdated</lastmod><changefreq>weekly</changefreq><priority>0.8</priority></url>"
            }
        } catch (e: Exception) {
            // Log error but return empty list to prevent sitemap failure
            println("Warning: Failed to fetch company pages for sitemap: ${e.message}")
            emptyList()
        }
    }

    private fun parseTimestamp(timestampStr: String): String {
        return try {
            val instant = if (timestampStr.contains("T")) {
                Instant.parse(timestampStr)
            } else {
                // Handle numeric timestamps
                Instant.ofEpochSecond(timestampStr.toLongOrNull() ?: 0)
            }
            instant.atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }
}
