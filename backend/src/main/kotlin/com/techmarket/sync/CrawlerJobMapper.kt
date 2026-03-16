package com.techmarket.sync

import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.model.JobRecord
import com.techmarket.sync.model.NormalizedJobDto
import com.techmarket.util.Constants.UNKNOWN_COUNTRY
import com.techmarket.util.Constants.UNKNOWN_LOCATION
import com.techmarket.util.IdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Maps NormalizedJobDto instances (from the crawler-service TypeScript response) to
 * JobRecord instances for persistence in the Silver Layer raw_jobs table.
 *
 * Reuses RawJobDataParser for technology extraction, location parsing, and seniority
 * detection so crawler-sourced jobs are consistent with Apify-sourced jobs.
 */
@Component
class CrawlerJobMapper(
    private val parser: RawJobDataParser,
) {
    private val log = LoggerFactory.getLogger(CrawlerJobMapper::class.java)

    fun toJobRecords(jobs: List<NormalizedJobDto>, companyId: String): List<JobRecord> =
        jobs.mapNotNull { job ->
            runCatching { mapOne(job, companyId) }
                .onFailure { log.error("Failed to map crawler job '${job.title}' for $companyId: ${it.message}") }
                .getOrNull()
        }

    private fun mapOne(job: NormalizedJobDto, companyId: String): JobRecord {
        val now = Instant.now()
        val location = job.location ?: ""

        val (city, _, parsedCountry) = parser.parseLocation(location)
        val country = when {
            parsedCountry != UNKNOWN_COUNTRY -> parsedCountry.lowercase()
            else -> parser.determineCountry(location).lowercase().takeIf { it != UNKNOWN_LOCATION.lowercase() } ?: "unknown"
        }

        val technologies = parser.extractTechnologies(
            "${job.title ?: ""} ${job.descriptionText ?: ""}"
        )

        val workModel = parser.extractWorkModel(location, job.title, job.descriptionText)
            .takeIf { job.workModel.isNullOrBlank() }
            ?: job.workModel
            ?: "On-site"

        val postedDate = job.postedAt?.let {
            runCatching { LocalDate.parse(it.substring(0, 10)) }.getOrNull()
        }
        val datePart = postedDate?.toString()
            ?: now.atZone(ZoneId.systemDefault()).toLocalDate().toString()

        val title = job.title?.trim()?.ifBlank { null } ?: "Unknown Title"
        val jobId = IdGenerator.buildJobId(companyId, country, title, datePart)

        val currency = job.salaryCurrency
            ?: NormalizedSalary.getDefaultCurrencyForCountry(country)

        return JobRecord(
            jobId = jobId,
            platformJobIds = listOfNotNull(job.platformId),
            applyUrls = listOfNotNull(job.applyUrl),
            platformLinks = listOfNotNull(job.platformUrl),
            locations = listOf(city).filter { it.isNotBlank() && it != UNKNOWN_LOCATION },
            companyId = companyId,
            companyName = job.companyName ?: "Unknown",
            source = "Crawler",
            country = country,
            city = city.takeIf { it != UNKNOWN_LOCATION } ?: "",
            stateRegion = "",
            title = title,
            seniorityLevel = parser.extractSeniority(title, job.seniorityLevel),
            technologies = technologies,
            salaryMin = job.salaryMin?.let { amount ->
                NormalizedSalary(
                    amount = (amount * 100).toLong(),
                    currency = currency,
                    period = NormalizedSalary.PERIOD_YEAR,
                    source = NormalizedSalary.SOURCE_JOB_POSTING,
                )
            },
            salaryMax = job.salaryMax?.let { amount ->
                NormalizedSalary(
                    amount = (amount * 100).toLong(),
                    currency = currency,
                    period = NormalizedSalary.PERIOD_YEAR,
                    source = NormalizedSalary.SOURCE_JOB_POSTING,
                )
            },
            postedDate = postedDate,
            benefits = emptyList(),
            employmentType = job.employmentType,
            workModel = workModel,
            jobFunction = job.department,
            description = job.descriptionText ?: job.descriptionHtml,
            lastSeenAt = now,
            urlStatus = "UNKNOWN",
        )
    }
}
