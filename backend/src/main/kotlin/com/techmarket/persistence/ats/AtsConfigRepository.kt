package com.techmarket.persistence.ats

import com.techmarket.persistence.model.CompanyAtsConfig
import com.techmarket.sync.ats.SyncStatus
import java.time.Instant

/** Repository interface for managing company ATS configurations. */
interface AtsConfigRepository {
    /** Returns all enabled ATS configurations across all companies. */
    fun getEnabledConfigs(): List<CompanyAtsConfig>

    /** Returns the ATS configuration for a specific company, if it exists. */
    fun getConfig(companyId: String): CompanyAtsConfig?

    /** Updates the status and timestamp of the last sync attempt for a company. */
    fun updateSyncStatus(companyId: String, status: SyncStatus, syncedAt: Instant)

    /** Saves or updates an ATS configuration. */
    fun saveConfig(config: CompanyAtsConfig)
}
