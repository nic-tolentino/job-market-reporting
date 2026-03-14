package com.techmarket.persistence.crawler

data class CrawlerSeedRecord(
    val companyId: String,
    val url: String,
    val category: String?,
    val status: String?,
    val paginationPattern: String?,
    val lastKnownJobCount: Int?,
    val lastKnownPageCount: Int?,
    val lastCrawledAt: String?,
    val lastDurationMs: Int?,
    val errorMessage: String?,
    val consecutiveZeroYieldCount: Int = 0,
    val atsProvider: String?,
    val atsIdentifier: String?,
    val atsDirectUrl: String?,
)
