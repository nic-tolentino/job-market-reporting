package com.techmarket.persistence.ingestion

import com.techmarket.persistence.model.RawIngestionRecord

interface IngestionRepository {
    fun saveRawIngestions(records: List<RawIngestionRecord>)
    fun getRawIngestions(): List<RawIngestionRecord>
}
