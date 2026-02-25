package com.jobmarket.app.persistence

import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.jobmarket.app.dashboard.model.TechTrendDto
import com.jobmarket.app.persistence.model.BigQueryJobRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("!local")
class JobBigQueryRepository(private val bigQueryTemplate: BigQueryTemplate) : JobRepository {

    private val log = LoggerFactory.getLogger(JobBigQueryRepository::class.java)

    // Configured via application.yml
    private val tableName = "raw_postings"

    override fun saveAll(jobs: List<BigQueryJobRecord>) {
        if (jobs.isEmpty()) return

        log.info("GCP: Streaming ${jobs.size} jobs to BigQuery table: $tableName")
        try {
            // BigQueryTemplate handles the batching and streaming insert into the dataset
            // configured in application.yml
            bigQueryTemplate.writeJsonStream(tableName, jobs.map { it.toMap() }.byteInputStream())
            log.info("GCP: Successfully inserted jobs into BigQuery.")
        } catch (e: Exception) {
            log.error("GCP: Failed to insert jobs into BigQuery: ${e.message}", e)
            throw e
        }
    }

    override fun getTechTrendsByWeek(monthsBack: Int): List<TechTrendDto> {
        // TODO: Implement the actual BigQuery SQL query to aggregate trends.
        // This will be built in Phase 3 when we confirm the dataset structure is live.
        log.warn("GCP: getTechTrendsByWeek SQL query is not yet implemented.")
        return emptyList()
    }

    /**
     * Helper extension to convert our Kotlin data class to a JSON-like Map which the BigQuery
     * template expects for streaming inserts.
     */
    private fun BigQueryJobRecord.toMap(): Map<String, Any?> {
        return mapOf(
                "job_id" to this.job_id,
                "source" to this.source,
                "country" to this.country,
                "title" to this.title,
                "company" to this.company,
                "location" to this.location,
                "seniority_level" to this.seniority_level,
                "technologies" to this.technologies,
                "salary_min" to this.salary_min,
                "salary_max" to this.salary_max,
                "posted_date" to
                        this.posted_date
                                ?.toString(), // Dates must be formatted as Strings for BQ JSON
                // inserts
                "raw_description" to this.raw_description,
                "companyLogo" to this.companyLogo,
                "benefits" to this.benefits,
                "applicantsCount" to this.applicantsCount,
                "applyUrl" to this.applyUrl,
                "jobPosterName" to this.jobPosterName,
                "employmentType" to this.employmentType,
                "jobFunction" to this.jobFunction,
                "industries" to this.industries,
                "companyDescription" to this.companyDescription,
                "companyWebsite" to this.companyWebsite,
                "companyEmployeesCount" to this.companyEmployeesCount,
                "raw_location" to this.raw_location,
                "raw_seniority_level" to this.raw_seniority_level,
                "ingested_at" to this.ingested_at.toString()
        )
    }

    // Helper to serialize a list of maps into an InputStream for the template
    private fun List<Map<String, Any?>>.byteInputStream(): java.io.InputStream {
        val jsonString =
                this.joinToString(separator = "\n") {
                    com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(it)
                }
        return jsonString.byteInputStream()
    }
}
