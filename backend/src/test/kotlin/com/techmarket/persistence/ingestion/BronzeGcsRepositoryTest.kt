package com.techmarket.persistence.ingestion

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.cloud.storage.Blob
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BronzeGcsRepositoryTest {

    private val gcsConfig = GcsConfig(
        bucketName = "test-bucket",
        projectId = "test-project",
        compressionEnabled = true
    )
    private val metadataRepository = mockk<IngestionMetadataRepository>()
    private val objectMapper = ObjectMapper()
    private val storage = mockk<Storage>()
    private val bucket = mockk<Bucket>()
    private val blob = mockk<Blob>()

    private lateinit var repository: BronzeGcsRepository

    @BeforeEach
    fun setup() {
        every { storage.get(gcsConfig.bucketName) } returns bucket
        repository = BronzeGcsRepository(gcsConfig, metadataRepository, objectMapper, storage)
    }

    @Test
    fun `saveIngestion uploads files to GCS and saves metadata`() {
        val manifest = createTestManifest()
        val dataChunks = listOf("{}".toByteArray(), "{}".toByteArray())

        every { bucket.create(any<String>(), any<ByteArray>()) } returns blob
        every { metadataRepository.saveManifest(any()) } returns Unit

        val result = repository.saveIngestion(manifest, dataChunks)

        verify(exactly = 2) { bucket.create(any<String>(), any<ByteArray>()) }
        verify { metadataRepository.saveManifest(any()) }
        assert(result.files.size == 2)
    }

    @Test
    fun `saveIngestion cleans up GCS files on metadata save failure`() {
        val manifest = createTestManifest()
        // Match dataChunks to manifest.fileCount (2 files)
        val dataChunks = listOf("{}".toByteArray(), "{}".toByteArray())

        // Mock successful upload
        every { bucket.create(any<String>(), any<ByteArray>()) } returns blob
        // Mock metadata save failure
        every { metadataRepository.saveManifest(any()) } throws RuntimeException("BigQuery error")
        // Mock cleanup
        every { storage.delete(any<com.google.cloud.storage.BlobId>()) } returns true

        assertThrows<RuntimeException> {
            repository.saveIngestion(manifest, dataChunks)
        }

        // Verify cleanup was attempted for both uploaded files
        verify(exactly = 2) { storage.delete(any<com.google.cloud.storage.BlobId>()) }
    }

    @Test
    fun `readFile returns decompressed stream`() {
        val filePath = "gs://test-bucket/apify/2026/03/10/dataset-123/jobs-0001.json.gz"
        val originalData = "{}"
        val compressedData = compressGzip(originalData.toByteArray())

        every { bucket.get("apify/2026/03/10/dataset-123/jobs-0001.json.gz") } returns blob
        every { blob.getContent() } returns compressedData

        val inputStream = repository.readFile(filePath)
        val decompressed = inputStream.readBytes().toString(Charsets.UTF_8)

        assert(decompressed == originalData)
    }

    @Test
    fun `isDatasetIngested delegates to metadata repository`() {
        every { metadataRepository.isDatasetIngested("dataset-123") } returns true

        val result = repository.isDatasetIngested("dataset-123")

        assert(result)
        verify { metadataRepository.isDatasetIngested("dataset-123") }
    }

    @Test
    fun `getManifest delegates to metadata repository`() {
        val expectedManifest = createTestManifest()
        every { metadataRepository.getManifest("dataset-123") } returns expectedManifest

        val result = repository.getManifest("dataset-123")

        assert(result == expectedManifest)
        verify { metadataRepository.getManifest("dataset-123") }
    }

    @Test
    fun `listManifests delegates to metadata repository with filters`() {
        val fromDate = Instant.now().minusSeconds(3600)
        val toDate = Instant.now()
        val expectedManifests = listOf(createTestManifest())

        every { metadataRepository.listManifests("apify", fromDate, toDate) } returns expectedManifests

        val result = repository.listManifests("apify", fromDate, toDate)

        assert(result == expectedManifests)
        verify { metadataRepository.listManifests("apify", fromDate, toDate) }
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

    private fun compressGzip(data: ByteArray): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        java.util.zip.GZIPOutputStream(outputStream).use { gzipOutputStream ->
            gzipOutputStream.write(data)
        }
        return outputStream.toByteArray()
    }
}
