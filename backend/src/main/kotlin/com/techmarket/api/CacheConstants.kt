package com.techmarket.api

object CacheConstants {
    const val CACHE_LANDING = "landing"
    const val CACHE_TECH = "tech"
    const val CACHE_COMPANY = "company"
    const val CACHE_SEARCH = "search"

    const val COUNTRY_KEY_PART = "#country ?: 'ALL'"
    const val COUNTRY_ONLY_KEY = COUNTRY_KEY_PART
    const val TECH_KEY = "{#techName, $COUNTRY_KEY_PART}"
    const val COMPANY_KEY = "{#companyId, $COUNTRY_KEY_PART}"
    const val SEARCH_KEY = "{#term ?: 'NO_TERM', $COUNTRY_KEY_PART}"
}
