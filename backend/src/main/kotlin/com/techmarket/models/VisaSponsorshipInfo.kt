package com.techmarket.models

import com.fasterxml.jackson.annotation.JsonAlias

data class VisaSponsorshipInfo(
    val offered: Boolean,
    val types: List<String> = emptyList(),
    val notes: String? = null,
    @com.fasterxml.jackson.annotation.JsonAlias("last_verified")
    val lastVerified: String? = null,
    val source: String? = null
)
