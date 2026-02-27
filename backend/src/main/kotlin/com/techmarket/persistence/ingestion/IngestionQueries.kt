package com.techmarket.persistence.ingestion

import com.techmarket.persistence.IngestionFields

object IngestionQueries {
    fun getSelectAllSql(datasetName: String, ingestionsTableName: String) =
            "SELECT ${IngestionFields.ID}, ${IngestionFields.SOURCE}, ${IngestionFields.INGESTED_AT}, ${IngestionFields.RAW_PAYLOAD} FROM `$datasetName.$ingestionsTableName`"
}
