package com.jobmarket.app.persistence

import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.jobmarket.app.dashboard.model.TechTrendDto
import com.jobmarket.app.persistence.model.CompanyRecord
import com.jobmarket.app.persistence.model.JobRecord
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("!local")
class JobBigQueryRepository(private val bigQueryTemplate: BigQueryTemplate) : JobRepository {

    private val log = LoggerFactory.getLogger(JobBigQueryRepository::class.java)

    private val jobsTableName = "raw_jobs"
    private val companiesTableName = "raw_companies"

    override fun saveJobs(jobs: List<JobRecord>) {
        if (jobs.isEmpty()) return

        log.info("GCP: Streaming ${jobs.size} jobs to BigQuery table: $jobsTableName")
        try {
            bigQueryTemplate.writeJsonStream(
                    jobsTableName,
                    jobs.map { it.toMap() }.byteInputStream()
            )
            log.info("GCP: Successfully inserted jobs into BigQuery.")
        } catch (e: Exception) {
            log.error("GCP: Failed to insert jobs into BigQuery: ${e.message}", e)
            throw e
        }
    }

    override fun saveCompanies(companies: List<CompanyRecord>) {
        if (companies.isEmpty()) return

        log.info(
                "GCP: Streaming ${companies.size} companies to BigQuery table: $companiesTableName"
        )
        try {
            bigQueryTemplate.writeJsonStream(
                    companiesTableName,
                    companies.map { it.toMap() }.byteInputStream()
            )
            log.info("GCP: Successfully inserted companies into BigQuery.")
        } catch (e: Exception) {
            log.error("GCP: Failed to insert companies into BigQuery: ${e.message}", e)
            throw e
        }
    }

    override fun getTechTrendsByWeek(monthsBack: Int): List<TechTrendDto> {
        // TODO: Implement the actual BigQuery SQL query to aggregate trends.
        log.warn("GCP: getTechTrendsByWeek SQL query is not yet implemented.")
        return emptyList()
    }

    private fun JobRecord.toMap(): Map<String, Any?> {
        return mapOf(
                "jobId" to this.jobId,
                "companyId" to this.companyId,
                "companyName" to this.companyName,
                "source" to this.source,
                "country" to this.country,
                "title" to this.title,
                "location" to this.location,
                "seniorityLevel" to this.seniorityLevel,
                "technologies" to this.technologies,
                "salaryMin" to this.salaryMin,
                "salaryMax" to this.salaryMax,
                "postedDate" to this.postedDate?.toString(),
                "benefits" to this.benefits,
                "employmentType" to this.employmentType,
                "jobFunction" to this.jobFunction,
                "applyUrl" to this.applyUrl,
                "rawLocation" to this.rawLocation,
                "rawSeniorityLevel" to this.rawSeniorityLevel,
                "ingestedAt" to this.ingestedAt.toString()
        )
    }

    private fun CompanyRecord.toMap(): Map<String, Any?> {
        return mapOf(
                "companyId" to this.companyId,
                "name" to this.name,
                "logoUrl" to this.logoUrl,
                "description" to this.description,
                "website" to this.website,
                "employeesCount" to this.employeesCount,
                "industries" to this.industries,
                "ingestedAt" to this.ingestedAt.toString()
        )
    }

    private fun List<Map<String, Any?>>.byteInputStream(): java.io.InputStream {
        val jsonString =
                this.joinToString(separator = "\n") {
                    com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(it)
                }
        return jsonString.byteInputStream()
    }
}
