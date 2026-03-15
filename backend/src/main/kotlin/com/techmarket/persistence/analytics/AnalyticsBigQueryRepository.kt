package com.techmarket.persistence.analytics

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.Schema
import com.google.cloud.bigquery.StandardSQLTypeName
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.api.model.*
import com.techmarket.persistence.AnalyticsFields
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.ensureTableExists
import java.time.Instant
import java.util.concurrent.TimeUnit
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class AnalyticsBigQueryRepository(
        bigQueryTemplateProvider: ObjectProvider<BigQueryTemplate>,
        bigQueryProvider: ObjectProvider<BigQuery>,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : AnalyticsRepository {

        private val bigQueryTemplate: BigQueryTemplate? = bigQueryTemplateProvider.ifAvailable
        private val bigQuery: BigQuery? = bigQueryProvider.ifAvailable

        private val log = LoggerFactory.getLogger(AnalyticsBigQueryRepository::class.java)

        private val jobsTableName = BigQueryTables.JOBS
        private val companiesTableName = BigQueryTables.COMPANIES
        private val searchMissesTableName = BigQueryTables.SEARCH_MISSES
        private val feedbackTableName = BigQueryTables.USER_FEEDBACK

        private fun ensureTables() {
                // search_misses schema
                val searchMissesSchema =
                        Schema.of(
                                Field.of(AnalyticsFields.TERM, StandardSQLTypeName.STRING),
                                Field.of(AnalyticsFields.TIMESTAMP, StandardSQLTypeName.TIMESTAMP)
                        )

                // user_feedback schema
                val feedbackSchema =
                        Schema.of(
                                Field.of(AnalyticsFields.CONTEXT, StandardSQLTypeName.STRING),
                                Field.of(AnalyticsFields.MESSAGE, StandardSQLTypeName.STRING),
                                Field.of(AnalyticsFields.TIMESTAMP, StandardSQLTypeName.TIMESTAMP)
                        )

                if (bigQuery == null) {
                        log.warn("BigQuery unavailable - skipping analytics table checks")
                        return
                }
                bigQuery.ensureTableExists(datasetName, searchMissesTableName, searchMissesSchema)
                bigQuery.ensureTableExists(datasetName, feedbackTableName, feedbackSchema)
        }

        override fun getLandingPageData(country: String?): LandingPageDto {
                val statsSql = AnalyticsQueries.getStatsSql(datasetName, jobsTableName)
                val topTechSql = AnalyticsQueries.getTopTechSql(datasetName, jobsTableName)
                val topCompaniesSql =
                        AnalyticsQueries.getTopCompaniesSql(
                                datasetName,
                                jobsTableName,
                                companiesTableName
                        )

                val statsConfig = QueryJobConfiguration.newBuilder(statsSql.sql)
                val techConfig = QueryJobConfiguration.newBuilder(topTechSql.sql)
                val companiesConfig = QueryJobConfiguration.newBuilder(topCompaniesSql.sql)

                val c = country?.lowercase()
                statsConfig.addNamedParameter(JobFields.COUNTRY, com.google.cloud.bigquery.QueryParameterValue.string(c))
                techConfig.addNamedParameter(JobFields.COUNTRY, com.google.cloud.bigquery.QueryParameterValue.string(c))
                companiesConfig.addNamedParameter(JobFields.COUNTRY, com.google.cloud.bigquery.QueryParameterValue.string(c))

                if (bigQuery == null) {
                    log.warn("BigQuery unavailable - returning empty landing page data")
                    return LandingPageDto(
                        globalStats = GlobalStatsDto(0, 0, 0, ""),
                        topTech = emptyList(),
                        topCompanies = emptyList()
                    )
                }
                val statsResult = bigQuery.query(statsConfig.build())
                val techResult = bigQuery.query(techConfig.build())
                val companiesResult = bigQuery.query(companiesConfig.build())

                return AnalyticsMapper.mapLandingPageData(statsResult, techResult, companiesResult)
        }

        override fun getSearchSuggestions(country: String?): SearchSuggestionsResponse {
                log.info("GCP: Querying search suggestions from BigQuery")
                val query =
                        AnalyticsQueries.getSearchSuggestionsSql(
                                datasetName,
                                companiesTableName,
                                jobsTableName
                        )

                val queryConfig = QueryJobConfiguration.newBuilder(query.sql)
                val c = country?.lowercase()
                queryConfig.addNamedParameter(JobFields.COUNTRY, com.google.cloud.bigquery.QueryParameterValue.string(c))

                if (bigQuery == null) return SearchSuggestionsResponse(emptyList())
                return try {
                        val result = bigQuery.query(queryConfig.build())
                        val suggestions =
                                result.values.map { AnalyticsMapper.mapSearchSuggestion(it) }
                        SearchSuggestionsResponse(suggestions.toList())
                } catch (e: Exception) {
                        log.error("GCP: Failed to fetch search suggestions: \${e.message}", e)
                        SearchSuggestionsResponse(emptyList())
                }
        }

        override fun saveSearchMiss(term: String) {
                ensureTables()
                log.info("GCP: Saving search miss to BigQuery: \$term")
                val record =
                        mapOf(
                                AnalyticsFields.TERM to term,
                                AnalyticsFields.TIMESTAMP to Instant.now().toString()
                        )
                if (bigQueryTemplate == null) {
                        log.warn("BigQueryTemplate unavailable - cannot save search miss")
                        return
                }
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        searchMissesTableName,
                                        listOf(record).byteInputStream()
                                )
                                .get(30, TimeUnit.SECONDS)
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert search miss: \${e.message}", e)
                }
        }

        override fun saveFeedback(context: String?, message: String) {
                ensureTables()
                log.info("GCP: Saving user feedback to BigQuery")
                val record =
                        mapOf(
                                AnalyticsFields.CONTEXT to context,
                                AnalyticsFields.MESSAGE to message,
                                AnalyticsFields.TIMESTAMP to Instant.now().toString()
                        )
                if (bigQueryTemplate == null) {
                        log.warn("BigQueryTemplate unavailable - cannot save feedback")
                        return
                }
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        feedbackTableName,
                                        listOf(record).byteInputStream()
                                )
                                .get(30, TimeUnit.SECONDS)
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert feedback: \${e.message}", e)
                }
        }

        override fun getAllFeedback(): List<com.techmarket.api.model.FeedbackDto> {
                val query = AnalyticsQueries.getFeedbackSql(datasetName, feedbackTableName)
                if (bigQuery == null) return emptyList()
                return try {
                        val result = bigQuery.query(QueryJobConfiguration.newBuilder(query.sql).build())
                        result.values.map { AnalyticsMapper.mapFeedback(it) }
                } catch (e: Exception) {
                        log.error("GCP: Failed to fetch user feedback: \${e.message}", e)
                        emptyList()
                }
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
