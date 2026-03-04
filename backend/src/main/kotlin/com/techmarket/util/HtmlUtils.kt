package com.techmarket.util

/** Utility for handling HTML content. */
object HtmlUtils {
    private val HTML_TAG_REGEX = Regex("<[^>]*>")

    private val WHITESPACE_REGEX = Regex("\\s+")

    /** Removes all HTML tags from a string. */
    fun stripHtml(html: String?): String? {
        if (html == null) return null
        return html.replace(HTML_TAG_REGEX, " ").replace(WHITESPACE_REGEX, " ").trim()
    }
}
