package com.techmarket.sync

import com.techmarket.persistence.job.JobRepository
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.NormalizedJobDto
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class CrawlerJobPersistenceServiceTest {

    private val mapper = mockk<CrawlerJobMapper>()
    private val merger = mockk<SilverDataMerger>()
    private val jobRepository = mockk<JobRepository>(relaxed = true)
    private val service = CrawlerJobPersistenceService(mapper, merger, jobRepository)

    // -------------------------------------------------------------------------
    // Short-circuit cases
    // -------------------------------------------------------------------------

    @Test
    fun `returns 0 and skips all calls when job list is empty`() {
        val result = service.persist("trademe", emptyList())
        assertEquals(0, result)
        verify { mapper wasNot Called }
        verify { jobRepository wasNot Called }
    }

    @Test
    fun `returns 0 when mapper produces no records`() {
        val rawJobs = listOf(job("Angular Developer"))
        every { mapper.toJobRecords(rawJobs, "trademe") } returns emptyList()

        val result = service.persist("trademe", rawJobs)

        assertEquals(0, result)
        verify { jobRepository wasNot Called }
    }

    // -------------------------------------------------------------------------
    // Happy path — new company (no existing records)
    // -------------------------------------------------------------------------

    @Test
    fun `inserts without merge when company has no existing jobs`() {
        val rawJobs = listOf(job("Angular Developer"), job("React Developer"))
        val mapped = listOf(record("trademe.nz.angular-developer.2026-03-16"), record("trademe.nz.react-developer.2026-03-16"))

        every { mapper.toJobRecords(rawJobs, "trademe") } returns mapped
        every { jobRepository.findByCompanyId("trademe") } returns emptyList()
        every { merger.mergeJobs(mapped, emptyList()) } returns mapped

        val result = service.persist("trademe", rawJobs)

        assertEquals(2, result)
        verify(exactly = 1) { jobRepository.saveJobs(mapped) }
    }

    // -------------------------------------------------------------------------
    // Deduplication — existing records present
    // -------------------------------------------------------------------------

    @Test
    fun `merges new records with existing before saving`() {
        val rawJobs = listOf(job("Angular Developer"))
        val newRecord = record("trademe.nz.angular-developer.2026-03-16")
        val existingRecord = record("trademe.nz.angular-developer.2026-03-16").copy(source = "LinkedIn")
        val mergedRecord = newRecord.copy(technologies = listOf("Angular", "TypeScript"))

        every { mapper.toJobRecords(rawJobs, "trademe") } returns listOf(newRecord)
        every { jobRepository.findByCompanyId("trademe") } returns listOf(existingRecord)
        every { merger.mergeJobs(listOf(newRecord), listOf(existingRecord)) } returns listOf(mergedRecord)

        val result = service.persist("trademe", rawJobs)

        assertEquals(1, result)
        verify(exactly = 1) { jobRepository.saveJobs(listOf(mergedRecord)) }
    }

    @Test
    fun `saves all merged records even when count differs from raw input`() {
        // 3 raw → 3 mapped → merged with 5 existing → 3 merged output (existing not re-saved)
        val rawJobs = (1..3).map { job("Job $it") }
        val mapped = (1..3).map { record("acme.nz.job-$it.2026-03-16") }
        val existing = (4..8).map { record("acme.nz.job-$it.2026-01-01") }
        val merged = mapped // merger returns only the new records in this scenario

        every { mapper.toJobRecords(rawJobs, "acme") } returns mapped
        every { jobRepository.findByCompanyId("acme") } returns existing
        every { merger.mergeJobs(mapped, existing) } returns merged

        val result = service.persist("acme", rawJobs)

        assertEquals(3, result)
        verify(exactly = 1) { jobRepository.saveJobs(merged) }
    }

    // -------------------------------------------------------------------------
    // Resilience — findByCompanyId failure
    // -------------------------------------------------------------------------

    @Test
    fun `proceeds with insert-only when findByCompanyId throws`() {
        val rawJobs = listOf(job("Developer"))
        val mapped = listOf(record("acme.nz.developer.2026-03-16"))

        every { mapper.toJobRecords(rawJobs, "acme") } returns mapped
        every { jobRepository.findByCompanyId("acme") } throws RuntimeException("BigQuery unavailable")
        every { merger.mergeJobs(mapped, emptyList()) } returns mapped

        val result = service.persist("acme", rawJobs)

        assertEquals(1, result)
        verify(exactly = 1) { jobRepository.saveJobs(mapped) }
    }

    // -------------------------------------------------------------------------
    // Failure — saveJobs throws (caller is responsible for handling)
    // -------------------------------------------------------------------------

    @Test
    fun `propagates exception when saveJobs fails`() {
        val rawJobs = listOf(job("Developer"))
        val mapped = listOf(record("acme.nz.developer.2026-03-16"))

        every { mapper.toJobRecords(rawJobs, "acme") } returns mapped
        every { jobRepository.findByCompanyId("acme") } returns emptyList()
        every { merger.mergeJobs(mapped, emptyList()) } returns mapped
        every { jobRepository.saveJobs(any()) } throws RuntimeException("stream write failed")

        assertThrows(RuntimeException::class.java) {
            service.persist("acme", rawJobs)
        }
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
        descriptionText = "We are hiring a $title",
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

    private fun record(jobId: String) = JobRecord(
        jobId = jobId,
        platformJobIds = emptyList(),
        applyUrls = emptyList(),
        platformLinks = emptyList(),
        locations = listOf("Auckland"),
        companyId = jobId.substringBefore('.'),
        companyName = "Acme",
        source = "Crawler",
        country = "nz",
        city = "Auckland",
        stateRegion = "",
        title = "Developer",
        seniorityLevel = "Mid-Level",
        technologies = emptyList(),
        salaryMin = null,
        salaryMax = null,
        postedDate = LocalDate.parse("2026-03-16"),
        benefits = emptyList(),
        employmentType = "Full-time",
        workModel = "On-site",
        jobFunction = null,
        description = null,
        lastSeenAt = Instant.now(),
        urlStatus = "UNKNOWN",
    )
}
