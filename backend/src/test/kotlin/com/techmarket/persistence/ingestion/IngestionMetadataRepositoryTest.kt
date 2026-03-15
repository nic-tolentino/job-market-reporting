package com.techmarket.persistence.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.TableResult
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.IngestionMetadataFields
import com.techmarket.persistence.ensureTableExists
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import org.springframework.beans.factory.ObjectProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.mockkStatic
import java.time.Instant
import java.util.concurrent.CompletableFuture
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class IngestionMetadataRepositoryTest {

    private val bigQueryTemplate = mockk<BigQueryTemplate>()
    private val bigQuery = mockk<BigQuery>(relaxed = true)
    private val bigQueryTemplateProvider = mockk<ObjectProvider<BigQueryTemplate>>()
    private val bigQueryProvider = mockk<ObjectProvider<BigQuery>>()
    private val objectMapper = ObjectMapper()
    private val datasetName = "techmarket"

    private lateinit var repository: IngestionMetadataRepository

    @BeforeEach
    fun setup() {
        mockkStatic("com.techmarket.persistence.BigQueryExtensionsKt")
        every { any<BigQuery>().ensureTableExists(any(), any(), any()) } returns Unit
        every { bigQueryTemplateProvider.ifAvailable } returns bigQueryTemplate
        every { bigQueryProvider.ifAvailable } returns bigQuery
        
        repository = IngestionMetadataRepository(bigQueryTemplateProvider, bigQueryProvider, objectMapper, datasetName)
    }

    // Note: saveManifest test skipped - requires integration test with real BigQuery
    // or complex mocking of BigQueryTemplate internals

    @Test
    fun `getManifest returns manifest when found`() {
        val manifest = createTestManifest()
        val tableResult = mockk<TableResult>()
        val fieldValueList = mockk<FieldValueList>()

        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns listOf(fieldValueList)
        setupFieldValueMocks(fieldValueList, manifest)

        val result = repository.getManifest("dataset-123")

        assert(result != null)
        assert(result!!.datasetId == manifest.datasetId)
    }

    @Test
    fun `getManifest returns null when not found`() {
        val tableResult = mockk<TableResult>()

        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns emptyList()

        val result = repository.getManifest("non-existent")

        assert(result == null)
    }

    @Test
    fun `isDatasetIngested returns true when exists`() {
        val tableResult = mockk<TableResult>()
        val fieldValueList = mockk<FieldValueList>()

        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns listOf(fieldValueList)

        val result = repository.isDatasetIngested("dataset-123")

        assert(result)
    }

    @Test
    fun `isDatasetIngested returns false when not exists`() {
        val tableResult = mockk<TableResult>()

        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns emptyList()

        val result = repository.isDatasetIngested("dataset-123")

        assert(!result)
    }

    @Test
    fun `listManifests returns filtered results`() {
        val manifest = createTestManifest()
        val tableResult = mockk<TableResult>()
        val fieldValueList = mockk<FieldValueList>()

        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns listOf(fieldValueList)
        setupFieldValueMocks(fieldValueList, manifest)

        val fromDate = Instant.now().minusSeconds(3600)
        val result = repository.listManifests(source = "apify", fromDate = fromDate, toDate = null)

        assert(result.size == 1)
        assert(result[0].datasetId == manifest.datasetId)
    }

    @Test
    fun `listManifests returns empty list when no results`() {
        val tableResult = mockk<TableResult>()

        every { bigQuery.query(any<QueryJobConfiguration>()) } returns tableResult
        every { tableResult.iterateAll() } returns emptyList()

        val result = repository.listManifests(source = null, fromDate = null, toDate = null)

        assert(result.isEmpty())
    }

    private fun setupFieldValueMocks(fieldValueList: FieldValueList, manifest: BronzeIngestionManifest) {
        val datasetIdField = mockk<FieldValue>()
        every { datasetIdField.stringValue } returns manifest.datasetId
        every { fieldValueList.get(IngestionMetadataFields.DATASET_ID) } returns datasetIdField
        
        val sourceField = mockk<FieldValue>()
        every { sourceField.stringValue } returns manifest.source
        every { fieldValueList.get(IngestionMetadataFields.SOURCE) } returns sourceField
        
        val ingestedAtField = mockk<FieldValue>()
        every { ingestedAtField.timestampValue } returns manifest.ingestedAt.toEpochMilli() * 1000
        every { fieldValueList.get(IngestionMetadataFields.INGESTED_AT) } returns ingestedAtField
        
        val targetCountryField = mockk<FieldValue>()
        every { targetCountryField.isNull } returns (manifest.targetCountry == null)
        manifest.targetCountry?.let { every { targetCountryField.stringValue } returns it }
        every { fieldValueList.get(IngestionMetadataFields.TARGET_COUNTRY) } returns targetCountryField
        
        val schemaVersionField = mockk<FieldValue>()
        every { schemaVersionField.stringValue } returns manifest.schemaVersion
        every { fieldValueList.get(IngestionMetadataFields.SCHEMA_VERSION) } returns schemaVersionField
        
        val recordCountField = mockk<FieldValue>()
        every { recordCountField.longValue } returns manifest.recordCount.toLong()
        every { fieldValueList.get(IngestionMetadataFields.RECORD_COUNT) } returns recordCountField
        
        val fileCountField = mockk<FieldValue>()
        every { fileCountField.longValue } returns manifest.fileCount.toLong()
        every { fieldValueList.get(IngestionMetadataFields.FILE_COUNT) } returns fileCountField
        
        val uncompressedSizeField = mockk<FieldValue>()
        every { uncompressedSizeField.longValue } returns manifest.uncompressedSizeBytes
        every { fieldValueList.get(IngestionMetadataFields.UNCOMPRESSED_SIZE_BYTES) } returns uncompressedSizeField
        
        val compressedSizeField = mockk<FieldValue>()
        every { compressedSizeField.longValue } returns manifest.compressedSizeBytes
        every { fieldValueList.get(IngestionMetadataFields.COMPRESSED_SIZE_BYTES) } returns compressedSizeField
        
        val compressionRatioField = mockk<FieldValue>()
        every { compressionRatioField.doubleValue } returns manifest.compressionRatio
        every { fieldValueList.get(IngestionMetadataFields.COMPRESSION_RATIO) } returns compressionRatioField
        
        val processingStatusField = mockk<FieldValue>()
        every { processingStatusField.stringValue } returns manifest.processingStatus.name
        every { fieldValueList.get(IngestionMetadataFields.PROCESSING_STATUS) } returns processingStatusField
        
        val filesField = mockk<FieldValue>()
        val fileMocks = manifest.files.map { f ->
            val fileMock = mockk<FieldValue>()
            every { fileMock.stringValue } returns f
            fileMock
        }
        every { filesField.repeatedValue } returns fileMocks
        every { fieldValueList.get(IngestionMetadataFields.FILES) } returns filesField
        
        val metadataIdField = mockk<FieldValue>()
        every { metadataIdField.isNull } returns (manifest.metadataId == null)
        manifest.metadataId?.let { every { metadataIdField.stringValue } returns it }
        every { fieldValueList.get(IngestionMetadataFields.METADATA_ID) } returns metadataIdField
    }

    private fun createTestManifest(): BronzeIngestionManifest {
        return BronzeIngestionManifest(
            datasetId = "dataset-123",
            source = "apify",
            ingestedAt = Instant.now(),
            targetCountry = "NZ",
            recordCount = 100,
            fileCount = 2,
            uncompressedSizeBytes = 10000,
            compressedSizeBytes = 2000,
            compressionRatio = 0.2,
            files = listOf(
                "gs://test-bucket/apify/2026/03/10/dataset-123/jobs-0001.json.gz",
                "gs://test-bucket/apify/2026/03/10/dataset-123/jobs-0002.json.gz"
            ),
            processingStatus = ProcessingStatus.PENDING
        )
    }
}
