package com.techmarket.persistence.ingestion

import com.techmarket.persistence.IngestionFields
import com.techmarket.persistence.model.RawIngestionRecord

object IngestionMapper {
    /** Pure function for mapping a BigQuery Row to a DTO, perfect for isolated unit testing */
    fun mapRow(row: com.google.cloud.bigquery.FieldValueList): RawIngestionRecord {
        val timestampMicros = row.get(IngestionFields.INGESTED_AT).timestampValue
        return RawIngestionRecord(
                id = row.get(IngestionFields.ID).stringValue,
                source = row.get(IngestionFields.SOURCE).stringValue,
                ingestedAt = java.time.Instant.ofEpochMilli(timestampMicros / 1000),
                rawPayload = row.get(IngestionFields.RAW_PAYLOAD).stringValue
        )
    }
}
