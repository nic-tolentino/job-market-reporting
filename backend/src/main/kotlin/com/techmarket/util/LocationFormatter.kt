package com.techmarket.util

object LocationFormatter {
    /**
     * Normalizes a location string by:
     * 1. Splitting by commas.
     * 2. Trimming whitespace.
     * 3. Removing empty/blank parts.
     * 4. Deduplicating adjacent identical parts (e.g., "Auckland, Auckland").
     * 5. Joining with ", ".
     */
    fun format(location: String?): String {
        if (location.isNullOrBlank()) return ""
        
        val parts = location.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        if (parts.isEmpty()) return ""
        
        val deduplicated = mutableListOf<String>()
        parts.forEach { part ->
            if (deduplicated.isEmpty() || deduplicated.last() != part) {
                deduplicated.add(part)
            }
        }
        
        return deduplicated.joinToString(", ")
    }
}
