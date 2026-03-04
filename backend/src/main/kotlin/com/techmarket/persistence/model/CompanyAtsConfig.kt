package com.techmarket.persistence.model

import com.techmarket.sync.ats.AtsProvider
import com.techmarket.sync.ats.SyncStatus
import java.time.Instant

/** Configuration for a company's direct ATS integration. */
data class CompanyAtsConfig(
        val companyId: String,
        val atsProvider: AtsProvider,
        val identifier: String, // e.g., Greenhouse board token, Lever company slug
        val enabled: Boolean,
        val lastSyncedAt: Instant?,
        val syncStatus: SyncStatus
)
