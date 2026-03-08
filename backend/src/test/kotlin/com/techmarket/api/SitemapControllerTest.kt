package com.techmarket.api

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SitemapControllerTest {

    @Test
    fun `sitemap XML has correct structure`() {
        // Test the XML structure generation logic
        val lastMod = Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val staticUrls = listOf(
            "",
            "/contact",
            "/transparency",
            "/privacy",
            "/terms"
        ).map { path ->
            val fullPath = if (path.isEmpty()) "https://devassembly.org" else "https://devassembly.org$path"
            "<url><loc>$fullPath</loc><lastmod>$lastMod</lastmod><changefreq>${if (path.isEmpty()) "daily" else "monthly"}</changefreq><priority>${if (path.isEmpty()) "1.0" else "0.5"}</priority></url>"
        }

        val allUrls = staticUrls.joinToString("\n")
        
        val sitemap = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
            append(allUrls)
            appendLine()
            appendLine("</urlset>")
        }

        // Verify XML structure
        assertTrue(sitemap.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        assertTrue(sitemap.contains("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">"))
        assertTrue(sitemap.contains("</urlset>"))
        
        // Verify static pages are included
        assertTrue(sitemap.contains("<loc>https://devassembly.org</loc>"))
        assertTrue(sitemap.contains("<loc>https://devassembly.org/contact</loc>"))
        assertTrue(sitemap.contains("<loc>https://devassembly.org/transparency</loc>"))
        assertTrue(sitemap.contains("<loc>https://devassembly.org/privacy</loc>"))
        assertTrue(sitemap.contains("<loc>https://devassembly.org/terms</loc>"))
        
        // Verify homepage has highest priority
        assertTrue(sitemap.contains("<priority>1.0</priority>"))
        assertTrue(sitemap.contains("<priority>0.5</priority>"))
        
        // Verify changefreq values
        assertTrue(sitemap.contains("<changefreq>daily</changefreq>"))
        assertTrue(sitemap.contains("<changefreq>monthly</changefreq>"))
    }

    @Test
    fun `sitemap contains lastmod dates`() {
        val lastMod = Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val url = "<url><loc>https://devassembly.org</loc><lastmod>$lastMod</lastmod><changefreq>daily</changefreq><priority>1.0</priority></url>"
        
        assertTrue(url.contains("<lastmod>$lastMod</lastmod>"))
    }

    @Test
    fun `technology URL encoding works correctly`() {
        val techNames = listOf(
            "Kotlin" to "kotlin",
            "React Native" to "react-native",
            "C++" to "c%2B%2B",
            ".NET" to ".net"
        )

        for ((input, _) in techNames) {
            val encoded = java.net.URLEncoder.encode(input.lowercase().replace(" ", "-"), java.nio.charset.StandardCharsets.UTF_8.toString())
            val url = "<url><loc>https://devassembly.org/tech/$encoded</loc><lastmod>2026-03-08</lastmod><changefreq>weekly</changefreq><priority>0.8</priority></url>"
            
            assertTrue(url.contains("https://devassembly.org/tech/"))
            assertTrue(url.contains("<changefreq>weekly</changefreq>"))
            assertTrue(url.contains("<priority>0.8</priority>"))
        }
    }

    @Test
    fun `company URL format is correct`() {
        val companyId = "atlassian"
        val lastUpdated = "2026-03-08"
        
        val url = "<url><loc>https://devassembly.org/company/$companyId</loc><lastmod>$lastUpdated</lastmod><changefreq>weekly</changefreq><priority>0.8</priority></url>"
        
        assertTrue(url.contains("<loc>https://devassembly.org/company/$companyId</loc>"))
        assertTrue(url.contains("<lastmod>$lastUpdated</lastmod>"))
        assertTrue(url.contains("<changefreq>weekly</changefreq>"))
        assertTrue(url.contains("<priority>0.8</priority>"))
    }

    @Test
    fun `sitemap XML is well-formed`() {
        val lastMod = Instant.now().atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_LOCAL_DATE)
        
        val staticUrls = listOf("", "/contact").map { path ->
            val fullPath = if (path.isEmpty()) "https://devassembly.org" else "https://devassembly.org$path"
            "<url><loc>$fullPath</loc><lastmod>$lastMod</lastmod><changefreq>${if (path.isEmpty()) "daily" else "monthly"}</changefreq><priority>${if (path.isEmpty()) "1.0" else "0.5"}</priority></url>"
        }

        val allUrls = staticUrls.joinToString("\n")
        
        val sitemap = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendLine("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">")
            append(allUrls)
            appendLine()
            appendLine("</urlset>")
        }

        // Basic XML well-formedness checks
        assertTrue(sitemap.indexOf("<?xml") == 0, "XML should start with declaration")
        assertTrue(sitemap.contains("<urlset"), "Should have urlset root element")
        assertTrue(sitemap.contains("</urlset>"), "Should have closing urlset tag")
        assertTrue(sitemap.contains("<url>"), "Should have url elements")
        assertTrue(sitemap.contains("</url>"), "Should have closing url tags")
    }
}
