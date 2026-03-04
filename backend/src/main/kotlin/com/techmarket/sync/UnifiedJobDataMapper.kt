package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.ats.model.NormalizedJob
import com.techmarket.util.IdGenerator
import com.techmarket.util.PiiSanitizer
import java.time.Instant
import java.time.ZoneId
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Maps [NormalizedJob] DTOs harvested from various ATS providers into structured [JobRecord] and
 * [CompanyRecord] entities for the Silver layer.
 */
@Service
class UnifiedJobDataMapper(private val parser: RawJobDataParser) {

        private val log = LoggerFactory.getLogger(UnifiedJobDataMapper::class.java)

        /** Maps a list of normalized job postings to a [MappedSyncData] container. */
        fun map(
                normalizedJobs: List<NormalizedJob>,
                companyId: String,
                syncTime: Instant
        ): MappedSyncData {
                val jobRecords = mutableListOf<JobRecord>()
                val companyRecords = mutableMapOf<String, CompanyRecord>()

                normalizedJobs.forEach { job ->
                        try {
                                val jobRecord = mapToJobRecord(job, companyId, syncTime)
                                jobRecords.add(jobRecord)

                                // Accumulate company metadata from job postings
                                if (!companyRecords.containsKey(companyId)) {
                                        companyRecords[companyId] =
                                                mapToCompanyRecord(job, companyId, syncTime)
                                } else {
                                        val existing = companyRecords[companyId]!!
                                        companyRecords[companyId] =
                                                existing.copy(
                                                        technologies =
                                                                (existing.technologies +
                                                                                jobRecord
                                                                                        .technologies)
                                                                        .distinct()
                                                                        .sorted(),
                                                        hiringLocations =
                                                                (existing.hiringLocations +
                                                                                jobRecord.locations)
                                                                        .distinct()
                                                                        .sorted(),
                                                        lastUpdatedAt = syncTime
                                                )
                                }
                        } catch (e: Exception) {
                                log.error(
                                        "Failed to map normalized job ${job.platformId} from ${job.source}: ${e.message}",
                                        e
                                )
                        }
                }

                return MappedSyncData(companyRecords.values.toList(), jobRecords)
        }

        private fun mapToJobRecord(
                job: NormalizedJob,
                companyId: String,
                syncTime: Instant
        ): JobRecord {
                val title = job.title ?: "Unknown Title"
                val (city, state, countryCode) = parser.parseLocation(job.location)
                val country =
                        if (countryCode != "Unknown") countryCode
                        else parser.determineCountry(job.location)

                // Generate a stable Job ID based on company, country, title, and posting date part
                val datePart =
                        (job.firstPublishedAt ?: job.postedAt)?.take(10)
                                ?: syncTime.atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val jobId = IdGenerator.buildJobId(companyId, country, title, datePart)

                val descriptionHtml = PiiSanitizer.sanitize(job.descriptionHtml)
                val technologies = parser.extractTechnologies(job.descriptionText ?: "")

                return JobRecord(
                        jobId = jobId,
                        platformJobIds = listOf(job.platformId),
                        applyUrls = listOfNotNull(job.applyUrl).distinct(),
                        platformLinks = listOfNotNull(job.platformUrl).distinct(),
                        locations = listOfNotNull(job.location).distinct().sorted(),
                        companyId = companyId,
                        companyName = job.companyName,
                        source = job.source,
                        country = country,
                        city = city,
                        stateRegion = if (state != "Unknown") state else "",
                        title = title,
                        seniorityLevel = parser.extractSeniority(title, job.seniorityLevel),
                        technologies = technologies,
                        salaryMin = job.salaryMin,
                        salaryMax = job.salaryMax,
                        postedDate = parser.parseDate(job.postedAt?.take(10)),
                        benefits =
                                emptyList(), // ATS APIs usually don't have structured benefits in a
                        // list
                        employmentType = job.employmentType,
                        workModel = job.workModel
                                        ?: parser.extractWorkModel(job.location, job.title),
                        jobFunction = job.department,
                        description = descriptionHtml ?: job.descriptionText ?: "",
                        lastSeenAt = syncTime
                )
        }

        private fun mapToCompanyRecord(
                job: NormalizedJob,
                companyId: String,
                syncTime: Instant
        ): CompanyRecord {
                return CompanyRecord(
                        companyId = companyId,
                        name = job.companyName,
                        alternateNames = listOf(job.companyName),
                        logoUrl = null, // ATS public APIs rarely expose company logo
                        description = null,
                        website = null,
                        employeesCount = null,
                        industries = null,
                        technologies = parser.extractTechnologies(job.descriptionText ?: ""),
                        hiringLocations = listOfNotNull(job.location).distinct().sorted(),
                        lastUpdatedAt = syncTime
                )
        }
}
