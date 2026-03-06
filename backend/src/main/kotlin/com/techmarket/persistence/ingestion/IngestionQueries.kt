package com.techmarket.persistence.ingestion

import com.techmarket.persistence.IngestionFields

object IngestionQueries {
    fun getSelectAllSql(datasetName: String, ingestionsTableName: String) =
            "SELECT ${IngestionFields.ID}, ${IngestionFields.SOURCE}, ${IngestionFields.INGESTED_AT}, ${IngestionFields.RAW_PAYLOAD}, ${IngestionFields.DATASET_ID} FROM `$datasetName.$ingestionsTableName`"

    fun getCheckDatasetSql(datasetName: String, ingestionsTableName: String, datasetId: String) =
            "SELECT 1 FROM `$datasetName.$ingestionsTableName` WHERE ${IngestionFields.DATASET_ID} = '$datasetId' LIMIT 1"
}
