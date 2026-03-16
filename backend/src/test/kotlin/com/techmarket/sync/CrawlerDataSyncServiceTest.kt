package com.techmarket.sync

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import com.techmarket.persistence.ingestion.GcsConfig
import com.techmarket.persistence.ingestion.IngestionMetadataRepository
import com.techmarket.persistence.model.BronzeIngestionManifest
import com.techmarket.persistence.model.ProcessingStatus
import com.techmarket.sync.model.NormalizedJobDto
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.util.zip.GZIPOutputStream

class CrawlerDataSyncServiceTest {

    private val metadataRepository = mockk<IngestionMetadataRepository>(relaxed = true)
    private val crawlerJobPersistenceService = mockk<CrawlerJobPersistenceService>(relaxed = true)
    private val objectMapper = jacksonObjectMapper()
    private val gcsConfig = GcsConfig(bucketName = "test-bucket", projectId = "test-project")
    private val storage = mockk<Storage>(relaxed = true)

    private val service = CrawlerDataSyncService(
        metadataRepository, crawlerJobPersistenceService, objectMapper, gcsConfig, storage
    )

    // -------------------------------------------------------------------------
    // archiveCrawlBatch
    // -------------------------------------------------------------------------

    @Test
    fun `archiveCrawlBatch skips GCS write when job list is empty`() {
        service.archiveCrawlBatch("trademe", "run-001", LocalDate.of(2026, 3, 16), emptyList())
        verify { storage wasNot Called }
    }

    @Test
    fun `archiveCrawlBatch writes gzip NDJSON to correct GCS path`() {
        val jobs = listOf(job("Software Engineer"), job("Backend Developer"))
        val blobInfoSlot = slot<BlobInfo>()
        val dataSlot = slot<ByteArray>()

        every { storage.create(capture(blobInfoSlot), capture(dataSlot)) } returns mockk()

        service.archiveCrawlBatch("trademe", "run-abc", LocalDate.of(2026, 3, 16), jobs)

        val captured = blobInfoSlot.captured
        assertEquals("test-bucket", captured.blobId.bucket)
        assertEquals("crawler/2026-03-16/run-run-abc/trademe-jobs.json.gz", captured.blobId.name)
        assertEquals("application/json", captured.contentType)
        assertEquals("gzip", captured.contentEncoding)

        // Verify the data is valid gzip-compressed NDJSON containing job data
        val decompressed = java.util.zip.GZIPInputStream(dataSlot.captured.inputStream())
            .bufferedReader().readText()
        assertTrue(decompressed.contains("Software Engineer"))
        assertTrue(decompressed.contains("Backend Developer"))
        assertEquals(2, decompressed.trim().lines().size)
    }

    // -------------------------------------------------------------------------
    // createDailyDataset
    // -------------------------------------------------------------------------

    @Test
    fun `createDailyDataset returns existing manifest when already ingested`() {
        val existingManifest = manifest("crawler-2026-03-16")
        every { metadataRepository.isDatasetIngested("crawler-2026-03-16") } returns true
        every { metadataRepository.getManifest("crawler-2026-03-16") } returns existingManifest

        val result = service.createDailyDataset(LocalDate.of(2026, 3, 16))

        assertEquals(existingManifest, result)
        verify { storage wasNot Called }
        verify(exactly = 0) { metadataRepository.saveManifest(any()) }
    }

    @Test
    fun `createDailyDataset returns null when no GCS files found`() {
        every { metadataRepository.isDatasetIngested(any()) } returns false
        every { storage.list("test-bucket", any()) } returns mockk {
            every { iterateAll() } returns emptyList()
        }

        val result = service.createDailyDataset(LocalDate.of(2026, 3, 16))

        assertNull(result)
        verify(exactly = 0) { metadataRepository.saveManifest(any()) }
    }

    @Test
    fun `createDailyDataset saves manifest and returns it when GCS files exist`() {
        val gcsPath = "crawler/2026-03-16/run-abc/trademe-jobs.json.gz"
        val mockBlob = mockk<Blob> {
            every { name } returns gcsPath
            every { size } returns 1024L
        }

        every { metadataRepository.isDatasetIngested("crawler-2026-03-16") } returns false
        every { storage.list("test-bucket", any()) } returns mockk {
            every { iterateAll() } returns listOf(mockBlob)
        }
        every { storage.get(any<BlobId>()) } returns mockBlob

        val manifestSlot = slot<BronzeIngestionManifest>()
        every { metadataRepository.saveManifest(capture(manifestSlot)) } just Runs

        val result = service.createDailyDataset(LocalDate.of(2026, 3, 16))

        assertNotNull(result)
        val saved = manifestSlot.captured
        assertEquals("crawler-2026-03-16", saved.datasetId)
        assertEquals("crawler", saved.source)
        assertEquals(1, saved.fileCount)
        assertEquals(ProcessingStatus.PENDING, saved.processingStatus)
        assertTrue(saved.files.contains("gs://test-bucket/$gcsPath"))
    }

    @Test
    fun `createDailyDataset filters out non-json-gz blobs`() {
        val validBlob = mockk<Blob> {
            every { name } returns "crawler/2026-03-16/run-abc/trademe-jobs.json.gz"
            every { size } returns 512L
        }
        val invalidBlob = mockk<Blob> {
            every { name } returns "crawler/2026-03-16/run-abc/manifest.txt"
            every { size } returns 100L
        }

        every { metadataRepository.isDatasetIngested(any()) } returns false
        every { storage.list("test-bucket", any()) } returns mockk {
            every { iterateAll() } returns listOf(validBlob, invalidBlob)
        }
        every { storage.get(any<BlobId>()) } returns validBlob

        val manifestSlot = slot<BronzeIngestionManifest>()
        every { metadataRepository.saveManifest(capture(manifestSlot)) } just Runs

        service.createDailyDataset(LocalDate.of(2026, 3, 16))

        assertEquals(1, manifestSlot.captured.fileCount)
        assertEquals(1, manifestSlot.captured.files.size)
    }

    // -------------------------------------------------------------------------
    // processDataset
    // -------------------------------------------------------------------------

    @Test
    fun `processDataset throws when manifest not found`() {
        every { metadataRepository.getManifest("crawler-2026-03-16") } returns null

        assertThrows(IllegalArgumentException::class.java) {
            service.processDataset("crawler-2026-03-16")
        }
    }

    @Test
    fun `processDataset throws when manifest source is not crawler`() {
        every { metadataRepository.getManifest("apify-xyz") } returns manifest("apify-xyz", source = "apify")

        assertThrows(IllegalArgumentException::class.java) {
            service.processDataset("apify-xyz")
        }
    }

    @Test
    fun `processDataset deserializes jobs and calls persistence service`() {
        val jobs = listOf(job("Software Engineer"), job("Frontend Developer"))
        val ndjson = jobs.joinToString("\n") { objectMapper.writeValueAsString(it) }
        val compressed = gzip(ndjson.toByteArray())

        val gcsPath = "gs://test-bucket/crawler/2026-03-16/run-abc/trademe-jobs.json.gz"
        val testManifest = manifest("crawler-2026-03-16", files = listOf(gcsPath))

        every { metadataRepository.getManifest("crawler-2026-03-16") } returns testManifest
        val mockBlob = mockk<Blob> {
            every { getContent() } returns compressed
        }
        every { storage.get(BlobId.of("test-bucket", "crawler/2026-03-16/run-abc/trademe-jobs.json.gz")) } returns mockBlob
        every { crawlerJobPersistenceService.persist("trademe", any()) } returns 2

        service.processDataset("crawler-2026-03-16")

        val jobsCaptor = slot<List<NormalizedJobDto>>()
        verify(exactly = 1) { crawlerJobPersistenceService.persist("trademe", capture(jobsCaptor)) }
        assertEquals(2, jobsCaptor.captured.size)
        assertEquals("Software Engineer", jobsCaptor.captured[0].title)

        verify { metadataRepository.updateProcessingStatus("crawler-2026-03-16", ProcessingStatus.COMPLETED) }
    }

    @Test
    fun `processDataset skips files with unparseable companyId`() {
        val gcsPath = "gs://test-bucket/crawler/2026-03-16/run-abc/bad-name.txt"
        val testManifest = manifest("crawler-2026-03-16", files = listOf(gcsPath))

        every { metadataRepository.getManifest("crawler-2026-03-16") } returns testManifest

        service.processDataset("crawler-2026-03-16")

        verify { crawlerJobPersistenceService wasNot Called }
        verify { metadataRepository.updateProcessingStatus("crawler-2026-03-16", ProcessingStatus.COMPLETED) }
    }

    @Test
    fun `processDataset skips missing GCS blobs without throwing`() {
        val gcsPath = "gs://test-bucket/crawler/2026-03-16/run-abc/trademe-jobs.json.gz"
        val testManifest = manifest("crawler-2026-03-16", files = listOf(gcsPath))

        every { metadataRepository.getManifest("crawler-2026-03-16") } returns testManifest
        every { storage.get(any<BlobId>()) } returns null

        service.processDataset("crawler-2026-03-16")

        verify { crawlerJobPersistenceService wasNot Called }
        verify { metadataRepository.updateProcessingStatus("crawler-2026-03-16", ProcessingStatus.COMPLETED) }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun job(title: String) = NormalizedJobDto(
        platformId = "pid-${title.hashCode()}",
        source = "Crawler",
        title = title,
        companyName = "Acme",
        location = "Auckland, NZ",
        descriptionHtml = null,
        descriptionText = null,
        salaryMin = null,
        salaryMax = null,
        salaryCurrency = null,
        employmentType = "Full-time",
        seniorityLevel = null,
        workModel = null,
        department = null,
        postedAt = "2026-03-16",
        applyUrl = null,
        platformUrl = null,
    )

    private fun manifest(
        datasetId: String,
        source: String = "crawler",
        files: List<String> = emptyList(),
    ) = BronzeIngestionManifest(
        datasetId = datasetId,
        source = source,
        ingestedAt = Instant.parse("2026-03-16T00:00:00Z"),
        targetCountry = null,
        recordCount = 0,
        fileCount = files.size,
        uncompressedSizeBytes = 0L,
        compressedSizeBytes = 0L,
        compressionRatio = 0.2,
        processingStatus = ProcessingStatus.PENDING,
        files = files,
    )

    private fun gzip(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(data) }
        return out.toByteArray()
    }
}
