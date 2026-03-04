package com.techmarket.persistence.ats

import com.google.cloud.bigquery.FieldValueList
import com.techmarket.persistence.AtsConfigFields
import com.techmarket.persistence.model.CompanyAtsConfig
import com.techmarket.sync.ats.AtsProvider
import com.techmarket.sync.ats.SyncStatus
import java.time.Instant

object AtsConfigMapper {
    fun mapRow(row: FieldValueList): CompanyAtsConfig {
        return CompanyAtsConfig(
                companyId = row[AtsConfigFields.COMPANY_ID].stringValue,
                atsProvider = AtsProvider.valueOf(row[AtsConfigFields.ATS_PROVIDER].stringValue),
                identifier = row[AtsConfigFields.IDENTIFIER].stringValue,
                enabled = row[AtsConfigFields.ENABLED].booleanValue,
                lastSyncedAt =
                        if (!row[AtsConfigFields.LAST_SYNCED_AT].isNull) {
                            Instant.ofEpochMilli(
                                    row[AtsConfigFields.LAST_SYNCED_AT].timestampValue / 1000
                            )
                        } else null,
                syncStatus = SyncStatus.valueOf(row[AtsConfigFields.SYNC_STATUS].stringValue)
        )
    }
}
