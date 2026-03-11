package com.techmarket.model

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/**
 * Top-level technology categories for domain hub pages and sector-level analytics.
 * Each category represents a distinct domain of the technology ecosystem.
 */
enum class TechCategory(
    val displayName: String,
    val description: String,
    val slug: String
) {
    LANGUAGES(
        "Languages",
        "Programming languages and scripting",
        "languages"
    ),
    FRONTEND(
        "Frontend",
        "UI frameworks and client-side development",
        "frontend"
    ),
    BACKEND(
        "Backend",
        "Server-side frameworks and APIs",
        "backend"
    ),
    MOBILE(
        "Mobile",
        "iOS, Android, and cross-platform development",
        "mobile"
    ),
    CLOUD_INFRA(
        "Cloud & Infra",
        "Cloud providers and infrastructure",
        "cloud-infra"
    ),
    DATA_AI(
        "Data & AI",
        "Databases, data engineering, and machine learning",
        "data-ai"
    ),
    DEVOPS(
        "DevOps",
        "CI/CD, containers, and automation",
        "devops"
    ),
    SECURITY(
        "Security",
        "Application and infrastructure security",
        "security"
    );

    companion object {
        /**
         * Converts a slug to a TechCategory.
         * @param slug the URL-friendly category identifier
         * @return the matching TechCategory
         * @throws ResponseStatusException (404) if slug doesn't match any category
         */
        fun fromSlug(slug: String): TechCategory {
            return entries.find { it.slug == slug }
                ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: $slug")
        }
    }
}
