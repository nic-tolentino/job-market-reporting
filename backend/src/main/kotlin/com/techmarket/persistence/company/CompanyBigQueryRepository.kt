package com.techmarket.persistence.company

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.cloud.bigquery.*
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.models.CompanyListingItem
import com.techmarket.models.CompanyRow
import com.techmarket.models.JobRow
import com.techmarket.models.VisaSponsorshipInfo
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
import org.springframework.beans.factory.ObjectProvider

@Repository
class CompanyBigQueryRepository(
        bigQueryTemplateProvider: ObjectProvider<BigQueryTemplate>,
        bigQueryProvider: ObjectProvider<BigQuery>,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : CompanyRepository {

        private val bigQueryTemplate: BigQueryTemplate? = bigQueryTemplateProvider.ifAvailable
        private val bigQuery: BigQuery? = bigQueryProvider.ifAvailable
        private val log = LoggerFactory.getLogger(CompanyBigQueryRepository::class.java)
        private val mapper = jacksonObjectMapper()

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
                                Field.of(CompanyFields.VISA_SPONSORSHIP_DETAIL, StandardSQLTypeName.JSON),
                                Field.of(CompanyFields.VERIFICATION_LEVEL, StandardSQLTypeName.STRING)
                        )
                if (bigQuery == null) {
                        log.warn("BigQuery unavailable - skipping table check")
                        return
                }
                bigQuery.ensureTableExists(datasetName, companiesTableName, schema)
        }

        override fun deleteAllCompanies() {
                ensureTable()
                val sql = "DELETE FROM `$datasetName.$companiesTableName` WHERE true"
                log.info("GCP: Deleting all rows from $companiesTableName using DML")
                if (bigQuery == null) {
                        log.warn("BigQuery unavailable - cannot delete companies")
                        return
                }
                try {
                        val queryConfig = QueryJobConfiguration.newBuilder(sql).build()
                        bigQuery.query(queryConfig)
                        log.info("GCP: Deleted all rows from $companiesTableName")
                } catch (e: Exception) {
                        log.error("GCP: Failed to delete companies: ${e.message}", e)
                        throw e
                }
        }

        override fun saveCompanies(companies: List<CompanyRecord>) {
                if (companies.isEmpty()) return
                ensureTable()
                log.info(
                        "GCP: Streaming ${companies.size} companies to BigQuery table: $companiesTableName"
                )
                if (bigQueryTemplate == null) {
                        log.warn("BigQueryTemplate unavailable - cannot stream companies")
                        return
                }
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        companiesTableName,
                                        companies.map { it.toMap() }.byteInputStream()
                                )
                                .get(120, TimeUnit.SECONDS)
                        log.info("GCP: Successfully inserted companies into BigQuery.")
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert companies into BigQuery: ${e.message}", e)
                        throw e
                }
        }

        override fun getAllCompanies(): List<CompanyRecord> {
                ensureTable()
                log.info("GCP: Fetching all companies from $companiesTableName")
                val query = "SELECT * FROM `$datasetName.$companiesTableName`"
                if (bigQuery == null) return emptyList()
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
                if (bigQuery == null) return emptyList()
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
                if (bigQuery == null) {
                        log.warn("BigQuery unavailable - cannot delete companies by ids")
                        return
                }
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

        // TODO: Point 5 - Move this to a Service layer as it returns an API DTO
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

                if (bigQuery == null) {
                        log.warn("BigQuery unavailable - returning mock profile for $companyId")
                        // Return a minimal valid DTO instead of throwing if in local/BigQuery-less mode
                        return CompanyMapper.mapCompanyProfile(
                            companyId, 
                            CompanyRow(companyId, companyId, website = "https://$companyId.com"), 
                            emptyList(), 
                            null
                        )
                }
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

        override fun getCompanyListing(visaOnly: Boolean, country: String?, limit: Int, offset: Int): List<CompanyListingItem> {
                ensureTable()
                val sql = """
                    SELECT 
                        c.${CompanyFields.COMPANY_ID} AS id,
                        c.${CompanyFields.NAME} AS name,
                        c.${CompanyFields.LOGO_URL} AS logoUrl,
                        c.${CompanyFields.VISA_SPONSORSHIP} AS visaSponsorshipLegacy,
                        ANY_VALUE(c.${CompanyFields.VISA_SPONSORSHIP_DETAIL}) AS visaSponsorshipDetail,
                        COUNT(DISTINCT j.${JobFields.JOB_ID}) AS activeRoles
                    FROM `$datasetName.$companiesTableName` c
                    LEFT JOIN `$datasetName.$jobsTableName` j ON c.${CompanyFields.COMPANY_ID} = j.${JobFields.COMPANY_ID}
                    WHERE (@country IS NULL OR c.${CompanyFields.HQ_COUNTRY} = @country OR @country IN UNNEST(c.${CompanyFields.OPERATING_COUNTRIES}))
                    GROUP BY id, name, logoUrl, visaSponsorshipLegacy
                    ORDER BY activeRoles DESC, name ASC
                    LIMIT @limit OFFSET @offset
                """.trimIndent()
                
                val c = country?.lowercase()
                
                val queryConfig = QueryJobConfiguration.newBuilder(sql)
                        .addNamedParameter(COUNTRY, QueryParameterValue.string(c))
                        .addNamedParameter("limit", QueryParameterValue.int64(limit.toLong()))
                        .addNamedParameter("offset", QueryParameterValue.int64(offset.toLong()))
                        .build()
                        
                if (bigQuery == null) return emptyList()
                val result = bigQuery.query(queryConfig)
                
                var items = result.iterateAll().map { row ->
                    val legacyVal = if (!row.get("visaSponsorshipLegacy").isNull) row.get("visaSponsorshipLegacy").booleanValue else false
                    val detailJson = if (!row.get("visaSponsorshipDetail").isNull) row.get("visaSponsorshipDetail").stringValue else null
                    
                    var visaInfo: VisaSponsorshipInfo? = null
                    if (detailJson != null) {
                        try {
                            visaInfo = mapper.readValue(detailJson, VisaSponsorshipInfo::class.java)
                        } catch (e: Exception) {
                            log.warn("Failed to parse visa_sponsorship_detail JSON for company ${row.get("id").stringValue}")
                        }
                    } else if (legacyVal) {
                        visaInfo = VisaSponsorshipInfo(offered = true)
                    }
                    
                    CompanyListingItem(
                        id = row.get("id").stringValue,
                        name = row.get("name").stringValue,
                        logo = if (!row.get("logoUrl").isNull) row.get("logoUrl").stringValue else "",
                        visaSponsorship = visaInfo,
                        activeRoles = row.get("activeRoles").numericValue.toInt()
                    )
                }
                
                // TODO: Push this filter to BigQuery SQL once transition is complete
                if (visaOnly) {
                    items = items.filter { it.visaSponsorship?.offered == true }
                }
                
                return items
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
                        CompanyFields.VISA_SPONSORSHIP to this.visaSponsorship?.offered,
                        CompanyFields.VISA_SPONSORSHIP_DETAIL to this.visaSponsorship?.let { visa -> mapper.writeValueAsString(visa) },
                        CompanyFields.VERIFICATION_LEVEL to this.verificationLevel,
                        CompanyFields.INGESTED_AT to this.lastUpdatedAt.toEpochMilli() * 1000,
                        CompanyFields.LAST_UPDATED_AT to this.lastUpdatedAt.toEpochMilli() * 1000
                ).filterValues { it != null }
        }

        private fun List<Map<String, Any?>>.byteInputStream(): java.io.InputStream {
                val jsonString = this.joinToString(separator = "\n") {
                    mapper.writeValueAsString(it)
                } + "\n" // NDJSON requirements
                return jsonString.byteInputStream()
        }
}
