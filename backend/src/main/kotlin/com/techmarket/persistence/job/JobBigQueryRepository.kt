package com.techmarket.persistence.job

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.ensureTableExists
import com.techmarket.persistence.model.JobRecord
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("!local")
class JobBigQueryRepository(
        private val bigQueryTemplate: BigQueryTemplate,
        private val bigQuery: BigQuery,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : JobRepository {

        private val log = LoggerFactory.getLogger(JobBigQueryRepository::class.java)

        private val jobsTableName = BigQueryTables.JOBS
        private val companiesTableName = BigQueryTables.COMPANIES

        private fun ensureTable() {
                val jobsSchema =
                        Schema.of(
                                Field.of(JobFields.JOB_ID, StandardSQLTypeName.STRING),
                                Field.of(JobFields.COMPANY_ID, StandardSQLTypeName.STRING),
                                Field.of(JobFields.COMPANY_NAME, StandardSQLTypeName.STRING),
                                Field.of(JobFields.SOURCE, StandardSQLTypeName.STRING),
                                Field.of(JobFields.COUNTRY, StandardSQLTypeName.STRING),
                                Field.of(JobFields.TITLE, StandardSQLTypeName.STRING),
                                Field.newBuilder(JobFields.LOCATIONS, StandardSQLTypeName.STRING)
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.newBuilder(JobFields.JOB_IDS, StandardSQLTypeName.STRING)
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.newBuilder(JobFields.APPLY_URLS, StandardSQLTypeName.STRING)
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.newBuilder(JobFields.LINKS, StandardSQLTypeName.STRING)
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.of(JobFields.SENIORITY_LEVEL, StandardSQLTypeName.STRING),
                                Field.newBuilder(JobFields.TECHNOLOGIES, StandardSQLTypeName.STRING)
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.of(JobFields.SALARY_MIN, StandardSQLTypeName.INT64),
                                Field.of(JobFields.SALARY_MAX, StandardSQLTypeName.INT64),
                                Field.of(JobFields.POSTED_DATE, StandardSQLTypeName.DATE),
                                Field.newBuilder(JobFields.BENEFITS, StandardSQLTypeName.STRING)
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.of(JobFields.EMPLOYMENT_TYPE, StandardSQLTypeName.STRING),
                                Field.of(JobFields.WORK_MODEL, StandardSQLTypeName.STRING),
                                Field.of(JobFields.JOB_FUNCTION, StandardSQLTypeName.STRING),
                                Field.of(JobFields.DESCRIPTION, StandardSQLTypeName.STRING),
                                Field.of(JobFields.CITY, StandardSQLTypeName.STRING),
                                Field.of(JobFields.STATE_REGION, StandardSQLTypeName.STRING),
                                Field.of(JobFields.INGESTED_AT, StandardSQLTypeName.TIMESTAMP)
                        )
                bigQuery.ensureTableExists(datasetName, jobsTableName, jobsSchema)
        }

        override fun deleteAllJobs() {
                ensureTable() // table may have been dropped externally during schema migrations
                log.info("GCP: Deleting all rows from \$jobsTableName")
                val query = "DELETE FROM `$datasetName.$jobsTableName` WHERE true"
                val queryConfig = QueryJobConfiguration.newBuilder(query).build()
                try {
                        bigQuery.query(queryConfig)
                        log.info("GCP: Successfully deleted all rows from \$jobsTableName")
                } catch (e: Exception) {
                        log.error(
                                "GCP: Failed to delete rows from \$jobsTableName: \${e.message}",
                                e
                        )
                        throw e
                }
        }

        override fun saveJobs(jobs: List<JobRecord>) {
                if (jobs.isEmpty()) return

                ensureTable()

                log.info("GCP: Streaming \${jobs.size} jobs to BigQuery table: \$jobsTableName")
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        jobsTableName,
                                        jobs.map { it.toMap() }.byteInputStream()
                                )
                                .get(60, TimeUnit.SECONDS)
                        log.info("GCP: Successfully inserted jobs into BigQuery.")
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert jobs into BigQuery: \${e.message}", e)
                        throw e
                }
        }

        private fun JobRecord.toMap(): Map<String, Any?> {
                return mapOf(
                        JobFields.JOB_IDS to this.jobIds,
                        JobFields.APPLY_URLS to this.applyUrls,
                        JobFields.LINKS to this.links,
                        JobFields.LOCATIONS to this.locations,
                        JobFields.COMPANY_ID to this.companyId,
                        JobFields.COMPANY_NAME to this.companyName,
                        JobFields.SOURCE to this.source,
                        JobFields.COUNTRY to this.country,
                        JobFields.TITLE to this.title,
                        JobFields.SENIORITY_LEVEL to this.seniorityLevel,
                        JobFields.TECHNOLOGIES to this.technologies,
                        JobFields.SALARY_MIN to this.salaryMin,
                        JobFields.SALARY_MAX to this.salaryMax,
                        JobFields.POSTED_DATE to this.postedDate?.toString(),
                        JobFields.BENEFITS to this.benefits,
                        JobFields.EMPLOYMENT_TYPE to this.employmentType,
                        JobFields.WORK_MODEL to this.workModel,
                        JobFields.JOB_FUNCTION to this.jobFunction,
                        JobFields.DESCRIPTION to this.description,
                        JobFields.CITY to this.city,
                        JobFields.STATE_REGION to this.stateRegion,
                        JobFields.INGESTED_AT to this.ingestedAt.toString()
                )
        }

        private fun List<Map<String, Any?>>.byteInputStream(): java.io.InputStream {
                val jsonString =
                        this.joinToString(separator = "\n") {
                                com.fasterxml
                                        .jackson
                                        .module
                                        .kotlin
                                        .jacksonObjectMapper()
                                        .writeValueAsString(it)
                        }
                return jsonString.byteInputStream()
        }

        override fun getJobDetails(jobId: String): com.techmarket.api.model.JobPageDto? {
                val detailsSql =
                        JobQueries.getDetailsSql(datasetName, jobsTableName, companiesTableName)
                val detResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(detailsSql)
                                        .addNamedParameter(
                                                "jobId",
                                                QueryParameterValue.string(jobId)
                                        )
                                        .build()
                        )

                val r = detResult.values.firstOrNull() ?: return null

                val techList =
                        if (r.get(JobFields.TECHNOLOGIES).isNull) emptyList<String>()
                        else r.get(JobFields.TECHNOLOGIES).repeatedValue.map { it.stringValue }

                val seniority = r.get(JobFields.SENIORITY_LEVEL).stringValue

                val similarSql = JobQueries.getSimilarSql(datasetName, jobsTableName, techList)
                val similarQueryBuilder =
                        QueryJobConfiguration.newBuilder(similarSql)
                                .addNamedParameter("jobId", QueryParameterValue.string(jobId))
                                .addNamedParameter(
                                        "seniority",
                                        QueryParameterValue.string(seniority)
                                )

                val similarResult = bigQuery.query(similarQueryBuilder.build())

                return JobMapper.mapJobDetails(r, techList, similarResult)
        }
}
