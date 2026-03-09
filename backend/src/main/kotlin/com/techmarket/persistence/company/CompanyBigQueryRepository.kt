package com.techmarket.persistence.company

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.models.CompanyRow
import com.techmarket.models.JobRow
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.QueryParams.COMPANY_ID
import com.techmarket.persistence.QueryParams.COUNTRY
import com.techmarket.persistence.getString
import com.techmarket.persistence.ensureTableExists
import com.techmarket.persistence.model.CompanyRecord
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class CompanyBigQueryRepository(
        private val bigQueryTemplate: BigQueryTemplate,
        private val bigQuery: BigQuery,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : CompanyRepository {

        private val log = LoggerFactory.getLogger(CompanyBigQueryRepository::class.java)

        private val jobsTableName = BigQueryTables.JOBS
        private val companiesTableName = BigQueryTables.COMPANIES

        private fun ensureTable() {
                val schema =
                        Schema.of(
                                Field.of(CompanyFields.COMPANY_ID, StandardSQLTypeName.STRING),
                                Field.of(CompanyFields.NAME, StandardSQLTypeName.STRING),
                                Field.newBuilder(
                                                CompanyFields.ALTERNATE_NAMES,
                                                StandardSQLTypeName.STRING
                                        )
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.of(CompanyFields.LOGO_URL, StandardSQLTypeName.STRING),
                                Field.of(CompanyFields.DESCRIPTION, StandardSQLTypeName.STRING),
                                Field.of(CompanyFields.WEBSITE, StandardSQLTypeName.STRING),
                                Field.of(CompanyFields.EMPLOYEES_COUNT, StandardSQLTypeName.INT64),
                                Field.of(CompanyFields.INDUSTRIES, StandardSQLTypeName.STRING),
                                Field.newBuilder(
                                                CompanyFields.TECHNOLOGIES,
                                                StandardSQLTypeName.STRING
                                        )
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.of(CompanyFields.INGESTED_AT, StandardSQLTypeName.TIMESTAMP),
                                Field.of(
                                        CompanyFields.LAST_UPDATED_AT,
                                        StandardSQLTypeName.TIMESTAMP
                                ),
                                Field.newBuilder(
                                                CompanyFields.HIRING_LOCATIONS,
                                                StandardSQLTypeName.STRING
                                        )
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.of(CompanyFields.IS_AGENCY, StandardSQLTypeName.BOOL),
                                Field.of(CompanyFields.IS_SOCIAL_ENTERPRISE, StandardSQLTypeName.BOOL),
                                Field.of(CompanyFields.HQ_COUNTRY, StandardSQLTypeName.STRING),
                                Field.newBuilder(
                                                CompanyFields.OPERATING_COUNTRIES,
                                                StandardSQLTypeName.STRING
                                        )
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.newBuilder(
                                                CompanyFields.OFFICE_LOCATIONS,
                                                StandardSQLTypeName.STRING
                                        )
                                        .setMode(Field.Mode.REPEATED)
                                        .build(),
                                Field.of(CompanyFields.REMOTE_POLICY, StandardSQLTypeName.STRING),
                                Field.of(CompanyFields.VISA_SPONSORSHIP, StandardSQLTypeName.BOOL),
                                Field.of(CompanyFields.VERIFICATION_LEVEL, StandardSQLTypeName.STRING)
                        )
                bigQuery.ensureTableExists(datasetName, companiesTableName, schema)
        }

        override fun deleteAllCompanies() {
                val tableId = com.google.cloud.bigquery.TableId.of(datasetName, companiesTableName)
                log.info("GCP: Dropping table $companiesTableName to allow schema refresh")
                try {
                        bigQuery.delete(tableId)
                        log.info("GCP: Successfully dropped table $companiesTableName")
                } catch (e: Exception) {
                        log.warn("GCP: Failed to drop table $companiesTableName (might not exist): ${e.message}")
                }
                ensureTable()
        }

        override fun saveCompanies(companies: List<CompanyRecord>) {
                if (companies.isEmpty()) return
                ensureTable()
                log.info(
                        "GCP: Streaming \${companies.size} companies to BigQuery table: \$companiesTableName"
                )
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        companiesTableName,
                                        companies.map { it.toMap() }.byteInputStream()
                                )
                                .get(120, TimeUnit.SECONDS)
                        log.info("GCP: Successfully inserted companies into BigQuery.")
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert companies into BigQuery: \${e.message}", e)
                        throw e
                }
        }

        override fun getAllCompanies(): List<CompanyRecord> {
                ensureTable()
                log.info("GCP: Fetching all companies from $companiesTableName")
                val query = "SELECT * FROM `$datasetName.$companiesTableName`"
                val queryConfig = QueryJobConfiguration.newBuilder(query).build()
                val result = bigQuery.query(queryConfig)
                return result.iterateAll().map { row -> 
                        CompanyMapper.mapToCompanyRecord(CompanyRow.fromCompanyRow(row)) 
                }
        }

        override fun getCompaniesByIds(companyIds: List<String>): List<CompanyRecord> {
                if (companyIds.isEmpty()) return emptyList()
                ensureTable()
                val sql =
                        "SELECT * FROM `$datasetName.$companiesTableName` WHERE ${CompanyFields.COMPANY_ID} IN UNNEST(?)"
                val queryConfig =
                        QueryJobConfiguration.newBuilder(sql)
                                .addPositionalParameter(
                                        QueryParameterValue.array(
                                                companyIds.toTypedArray(),
                                                StandardSQLTypeName.STRING
                                        )
                                )
                                .build()
                val result = bigQuery.query(queryConfig)
                return result.iterateAll().map { row -> 
                        CompanyMapper.mapToCompanyRecord(CompanyRow.fromCompanyRow(row)) 
                }
        }

        override fun deleteCompaniesByIds(companyIds: List<String>) {
                if (companyIds.isEmpty()) return
                ensureTable()
                val sql =
                        "DELETE FROM `$datasetName.$companiesTableName` WHERE ${CompanyFields.COMPANY_ID} IN UNNEST(?)"
                val queryConfig =
                        QueryJobConfiguration.newBuilder(sql)
                                .addPositionalParameter(
                                        QueryParameterValue.array(
                                                companyIds.toTypedArray(),
                                                StandardSQLTypeName.STRING
                                        )
                                )
                                .build()
                try {
                        bigQuery.query(queryConfig)
                        log.info(
                                "GCP: Deleted ${companyIds.size} companies from $companiesTableName"
                        )
                } catch (e: Exception) {
                        log.error("GCP: Failed to delete companies: ${e.message}", e)
                        throw e
                }
        }

        override fun getCompanyProfile(companyId: String, country: String?): CompanyProfilePageDto {
                val detailsQuery = CompanyQueries.getDetailsSql(datasetName, companiesTableName)
                val jobsQuery = CompanyQueries.getJobsSql(datasetName, jobsTableName)
                val aggQuery = CompanyQueries.getAggSql(datasetName, jobsTableName)

                val detConfig = QueryJobConfiguration.newBuilder(detailsQuery.sql)
                        .addNamedParameter(COMPANY_ID, QueryParameterValue.string(companyId))
                val jobsConfig = QueryJobConfiguration.newBuilder(jobsQuery.sql)
                        .addNamedParameter(COMPANY_ID, QueryParameterValue.string(companyId))
                val aggConfig = QueryJobConfiguration.newBuilder(aggQuery.sql)
                        .addNamedParameter(COMPANY_ID, QueryParameterValue.string(companyId))

                // Always add country parameter (as NULL if not provided) to satisfy BigQuery SQL
                // The SQL uses: AND (@country IS NULL OR country = @country)
                // This pattern requires the parameter to always exist, even if NULL
                val c = country?.lowercase()
                detConfig.addNamedParameter(COUNTRY, QueryParameterValue.string(c))
                jobsConfig.addNamedParameter(COUNTRY, QueryParameterValue.string(c))
                aggConfig.addNamedParameter(COUNTRY, QueryParameterValue.string(c))

                val detResult = bigQuery.query(detConfig.build())
                val jobsResult = bigQuery.query(jobsConfig.build())
                val aggResult = bigQuery.query(aggConfig.build())

                // Hydrate typed rows from BigQuery results
                val companyRow = detResult.values.firstOrNull()?.let { CompanyRow.fromCompanyRow(it) }
                        ?: throw CompanyNotFoundException(companyId)
                
                val jobRows = jobsResult.values.map { JobRow.fromJobRow(it) }
                
                val topModel = aggResult.values.firstOrNull()?.getString("topModel")

                return CompanyMapper.mapCompanyProfile(companyId, companyRow, jobRows, topModel)
        }

        private fun CompanyRecord.toMap(): Map<String, Any?> {
                return mapOf(
                        CompanyFields.COMPANY_ID to this.companyId,
                        CompanyFields.NAME to this.name,
                        CompanyFields.ALTERNATE_NAMES to this.alternateNames,
                        CompanyFields.LOGO_URL to this.logoUrl,
                        CompanyFields.DESCRIPTION to this.description,
                        CompanyFields.WEBSITE to this.website,
                        CompanyFields.EMPLOYEES_COUNT to this.employeesCount,
                        CompanyFields.INDUSTRIES to this.industries,
                        CompanyFields.TECHNOLOGIES to this.technologies,
                        CompanyFields.HIRING_LOCATIONS to this.hiringLocations,
                        CompanyFields.IS_AGENCY to this.isAgency,
                        CompanyFields.IS_SOCIAL_ENTERPRISE to this.isSocialEnterprise,
                        CompanyFields.HQ_COUNTRY to this.hqCountry,
                        CompanyFields.OPERATING_COUNTRIES to this.operatingCountries,
                        CompanyFields.OFFICE_LOCATIONS to this.officeLocations,
                        CompanyFields.REMOTE_POLICY to this.remotePolicy,
                        CompanyFields.VISA_SPONSORSHIP to this.visaSponsorship,
                        CompanyFields.VERIFICATION_LEVEL to this.verificationLevel,
                        CompanyFields.INGESTED_AT to this.lastUpdatedAt.toString(),
                        CompanyFields.LAST_UPDATED_AT to this.lastUpdatedAt.toString()
                ).filterValues { it != null }
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
}
