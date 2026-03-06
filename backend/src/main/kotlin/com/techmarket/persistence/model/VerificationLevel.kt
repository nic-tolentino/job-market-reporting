package com.techmarket.persistence.model

import com.fasterxml.jackson.annotation.JsonValue

/**
 * Represents the verification status of a company in the ecosystem.
 * Values are used for filtering (e.g., blocking spam) and frontend badges.
 */
enum class VerificationLevel(@get:JsonValue val value: String) {
    VERIFIED("verified"),               // Human-curated "Gold" standard
    COMMUNITY_VERIFIED("community_verified"), // Validated by users
    SILVER("silver"),                   // AI-enriched with high confidence
    UNVERIFIED("unverified"),           // Automatically discovered, needs enrichment
    STALE("stale"),                     // Previously verified but data is old
    NEEDS_REVIEW("needs_review"),       // Flagged for manual audit
    BLOCKED("blocked");                 // Junk or spam, excluded from search results

    companion object {
        fun fromString(value: String): VerificationLevel {
            return values().find { it.value == value.lowercase() } ?: UNVERIFIED
        }
    }
}
