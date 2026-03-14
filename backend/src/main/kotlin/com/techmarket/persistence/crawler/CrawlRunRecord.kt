package com.techmarket.persistence.crawler

data class CrawlRunRecord(
    val runId: String,
    val batchId: String?,
    val companyId: String,
    val seedUrl: String,
    val isTargeted: Boolean,
    val startedAt: String,
    val durationMs: Int?,
    val pagesVisited: Int?,
    val jobsRaw: Int?,
    val jobsValid: Int?,
    val jobsTech: Int?,
    val jobsFinal: Int?,
    val confidenceAvg: Double?,
    val atsProvider: String?,
    val atsIdentifier: String?,
    val atsDirectUrl: String?,
    val paginationPattern: String?,
    val status: String,
    val errorMessage: String?,
    val modelUsed: String?,
)
