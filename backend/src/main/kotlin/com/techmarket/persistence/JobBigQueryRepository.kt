package com.techmarket.persistence

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.google.cloud.spring.bigquery.core.BigQueryTemplate
import com.techmarket.api.model.*
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import com.techmarket.persistence.model.RawIngestionRecord
import java.time.Instant
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

        private val jobsTableName = "raw_jobs"
        private val companiesTableName = "raw_companies"
        private val ingestionsTableName = "raw_ingestions"
        private val searchMissesTableName = "search_misses"
        private val feedbackTableName = "user_feedback"

        private fun ensureTableExists(tableName: String, schema: com.google.cloud.bigquery.Schema) {
                val tableId = com.google.cloud.bigquery.TableId.of(datasetName, tableName)
                val table = bigQuery.getTable(tableId)
                if (table == null) {
                        log.info(
                                "GCP: Table $tableName not found in dataset $datasetName. Creating now..."
                        )
                        val tableDefinition =
                                com.google.cloud.bigquery.StandardTableDefinition.of(schema)
                        val tableInfo =
                                com.google.cloud.bigquery.TableInfo.newBuilder(
                                                tableId,
                                                tableDefinition
                                        )
                                        .build()
                        bigQuery.create(tableInfo)
                        log.info("GCP: Table $tableName created successfully.")
                }
        }

        private fun ensureAllTables() {
                // raw_ingestions schema
                val ingestionsSchema =
                        com.google.cloud.bigquery.Schema.of(
                                com.google.cloud.bigquery.Field.of(
                                        "id",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "source",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "ingestedAt",
                                        com.google.cloud.bigquery.StandardSQLTypeName.TIMESTAMP
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "rawPayload",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                )
                        )
                ensureTableExists(ingestionsTableName, ingestionsSchema)

                // raw_companies schema
                val companiesSchema =
                        com.google.cloud.bigquery.Schema.of(
                                com.google.cloud.bigquery.Field.of(
                                        "companyId",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "name",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "logoUrl",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "description",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "website",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "employeesCount",
                                        com.google.cloud.bigquery.StandardSQLTypeName.INT64
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "industries",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.newBuilder(
                                                "technologies",
                                                com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                        )
                                        .setMode(com.google.cloud.bigquery.Field.Mode.REPEATED)
                                        .build(),
                                com.google.cloud.bigquery.Field.of(
                                        "ingestedAt",
                                        com.google.cloud.bigquery.StandardSQLTypeName.TIMESTAMP
                                ),
                                com.google.cloud.bigquery.Field.newBuilder(
                                                "hiringLocations",
                                                com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                        )
                                        .setMode(com.google.cloud.bigquery.Field.Mode.REPEATED)
                                        .build()
                        )
                ensureTableExists(companiesTableName, companiesSchema)

                // raw_jobs schema
                val jobsSchema =
                        com.google.cloud.bigquery.Schema.of(
                                com.google.cloud.bigquery.Field.of(
                                        "jobId",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "companyId",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "companyName",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "source",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "country",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "title",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.newBuilder(
                                                "locations",
                                                com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                        )
                                        .setMode(com.google.cloud.bigquery.Field.Mode.REPEATED)
                                        .build(),
                                com.google.cloud.bigquery.Field.newBuilder(
                                                "jobIds",
                                                com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                        )
                                        .setMode(com.google.cloud.bigquery.Field.Mode.REPEATED)
                                        .build(),
                                com.google.cloud.bigquery.Field.newBuilder(
                                                "applyUrls",
                                                com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                        )
                                        .setMode(com.google.cloud.bigquery.Field.Mode.REPEATED)
                                        .build(),
                                com.google.cloud.bigquery.Field.of(
                                        "seniorityLevel",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.newBuilder(
                                                "technologies",
                                                com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                        )
                                        .setMode(com.google.cloud.bigquery.Field.Mode.REPEATED)
                                        .build(),
                                com.google.cloud.bigquery.Field.of(
                                        "salaryMin",
                                        com.google.cloud.bigquery.StandardSQLTypeName.INT64
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "salaryMax",
                                        com.google.cloud.bigquery.StandardSQLTypeName.INT64
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "postedDate",
                                        com.google.cloud.bigquery.StandardSQLTypeName.DATE
                                ),
                                com.google.cloud.bigquery.Field.newBuilder(
                                                "benefits",
                                                com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                        )
                                        .setMode(com.google.cloud.bigquery.Field.Mode.REPEATED)
                                        .build(),
                                com.google.cloud.bigquery.Field.of(
                                        "employmentType",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "workModel",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "jobFunction",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "rawLocation",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "rawSeniorityLevel",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "ingestedAt",
                                        com.google.cloud.bigquery.StandardSQLTypeName.TIMESTAMP
                                )
                        )
                ensureTableExists(jobsTableName, jobsSchema)

                // search_misses schema
                val searchMissesSchema =
                        com.google.cloud.bigquery.Schema.of(
                                com.google.cloud.bigquery.Field.of(
                                        "term",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "timestamp",
                                        com.google.cloud.bigquery.StandardSQLTypeName.TIMESTAMP
                                )
                        )
                ensureTableExists(searchMissesTableName, searchMissesSchema)

                // user_feedback schema
                val feedbackSchema =
                        com.google.cloud.bigquery.Schema.of(
                                com.google.cloud.bigquery.Field.of(
                                        "context",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "message",
                                        com.google.cloud.bigquery.StandardSQLTypeName.STRING
                                ),
                                com.google.cloud.bigquery.Field.of(
                                        "timestamp",
                                        com.google.cloud.bigquery.StandardSQLTypeName.TIMESTAMP
                                )
                        )
                ensureTableExists(feedbackTableName, feedbackSchema)
        }

        override fun saveRawIngestions(records: List<RawIngestionRecord>) {
                if (records.isEmpty()) return

                ensureAllTables()

                log.info(
                        "GCP: Streaming ${records.size} raw ingestion records to BigQuery table: $ingestionsTableName"
                )
                try {
                        // IMPORTANT: We must call .get() to block on the CompletableFuture returned
                        // by writeJsonStream. The BigQueryTemplate uses an internal
                        // ThreadPoolTaskScheduler to poll the BigQuery Storage Write API (gRPC)
                        // for a result. If the future is discarded (fire-and-forget), that
                        // scheduler
                        // can be destroyed by Spring/Cloud Run before the write completes, causing:
                        //   TaskRejectedException: ExecutorService in shutdown state
                        // Blocking here ensures the write is fully acknowledged before we return.
                        // A 60s timeout is generous but prevents an indefinite hang if GCP is slow.
                        bigQueryTemplate
                                .writeJsonStream(
                                        ingestionsTableName,
                                        records.map { it.toMap() }.byteInputStream()
                                )
                                .get(
                                        60,
                                        TimeUnit.SECONDS
                                ) // Block until write is acknowledged — see comment above
                        log.info("GCP: Successfully inserted raw ingestion records to BigQuery.")
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert raw ingestion records: ${e.message}", e)
                        throw e
                }
        }

        override fun getRawIngestions(): List<RawIngestionRecord> {
                ensureAllTables()
                log.info("GCP: Fetching all raw ingestions for reprocessing")
                val query =
                        "SELECT id, source, ingestedAt, rawPayload FROM $datasetName.$ingestionsTableName"
                val queryConfig = QueryJobConfiguration.newBuilder(query).build()

                return try {
                        val results = bigQuery.query(queryConfig)
                        results.iterateAll()
                                .map { row ->
                                        val timestampMicros = row.get("ingestedAt").timestampValue
                                        val ingestedAt =
                                                java.time.Instant.ofEpochMilli(
                                                        timestampMicros / 1000
                                                )

                                        RawIngestionRecord(
                                                id = row.get("id").stringValue,
                                                source = row.get("source").stringValue,
                                                ingestedAt = ingestedAt,
                                                rawPayload = row.get("rawPayload").stringValue
                                        )
                                }
                                .toList()
                } catch (e: Exception) {
                        log.error("GCP: Failed to fetch raw ingestions: ${e.message}", e)
                        emptyList()
                }
        }

        override fun deleteAllJobs() {
                log.info("GCP: Deleting all rows from $jobsTableName")
                val query = "DELETE FROM $datasetName.$jobsTableName WHERE true"
                val queryConfig = QueryJobConfiguration.newBuilder(query).build()
                try {
                        bigQuery.query(queryConfig)
                        log.info("GCP: Successfully deleted all rows from $jobsTableName")
                } catch (e: Exception) {
                        log.error("GCP: Failed to delete rows from $jobsTableName: ${e.message}", e)
                        throw e
                }
        }

        override fun saveJobs(jobs: List<JobRecord>) {
                if (jobs.isEmpty()) return

                ensureAllTables()

                log.info("GCP: Streaming ${jobs.size} jobs to BigQuery table: $jobsTableName")
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        jobsTableName,
                                        jobs.map { it.toMap() }.byteInputStream()
                                )
                                .get(
                                        60,
                                        TimeUnit.SECONDS
                                ) // Block until write is acknowledged — see saveRawIngestions for
                        // rationale
                        log.info("GCP: Successfully inserted jobs into BigQuery.")
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert jobs into BigQuery: ${e.message}", e)
                        throw e
                }
        }

        override fun getLandingPageData(): LandingPageDto {
                val statsSql =
                        """
            SELECT 
                COUNT(*) as totalVacancies,
                IFNULL(SUM(IF(workModel = 'Remote', 1, 0)), 0) as remoteCount,
                IFNULL(SUM(IF(workModel = 'Hybrid', 1, 0)), 0) as hybridCount
            FROM `$datasetName.$jobsTableName`
            WHERE DATE(postedDate) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
        """.trimIndent()

                val topTechSql =
                        """
            SELECT t as name, COUNT(*) as count
            FROM `$datasetName.$jobsTableName`, UNNEST(technologies) as t
            WHERE DATE(postedDate) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
            GROUP BY name
            ORDER BY count DESC
            LIMIT 5
        """.trimIndent()

                val topCompaniesSql =
                        """
            SELECT c.companyId as id, MAX(c.name) as name, MAX(c.logoUrl) as logo, COUNT(*) as activeRoles
            FROM `$datasetName.$jobsTableName` j
            JOIN `$datasetName.$companiesTableName` c ON j.companyId = c.companyId
            WHERE DATE(j.postedDate) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
            GROUP BY c.companyId
            ORDER BY activeRoles DESC
            LIMIT 5
        """.trimIndent()

                val statsResult = bigQuery.query(QueryJobConfiguration.newBuilder(statsSql).build())
                val techResult =
                        bigQuery.query(QueryJobConfiguration.newBuilder(topTechSql).build())
                val companiesResult =
                        bigQuery.query(QueryJobConfiguration.newBuilder(topCompaniesSql).build())

                val statsRow = statsResult.values.firstOrNull()
                val totalVacancies = statsRow?.get("totalVacancies")?.longValue?.toInt() ?: 0
                val remoteCount = statsRow?.get("remoteCount")?.longValue?.toInt() ?: 0
                val hybridCount = statsRow?.get("hybridCount")?.longValue?.toInt() ?: 0

                val remotePct = if (totalVacancies > 0) (remoteCount * 100) / totalVacancies else 0
                val hybridPct = if (totalVacancies > 0) (hybridCount * 100) / totalVacancies else 0

                val topTechList =
                        techResult.values.map { row ->
                                TechTrendAggregatedDto(
                                        name = row.get("name").stringValue,
                                        count = row.get("count").longValue.toInt(),
                                        percentageChange = 0.0
                                )
                        }
                val topTechName = topTechList.firstOrNull()?.name ?: "N/A"

                val topCompaniesList =
                        companiesResult.values.map { row ->
                                CompanyLeaderboardDto(
                                        id = row.get("id").stringValue,
                                        name = row.get("name").stringValue,
                                        logo =
                                                if (row.get("logo").isNull) ""
                                                else row.get("logo").stringValue,
                                        activeRoles = row.get("activeRoles").longValue.toInt()
                                )
                        }

                return LandingPageDto(
                        globalStats =
                                GlobalStatsDto(totalVacancies, remotePct, hybridPct, topTechName),
                        topTech = topTechList,
                        topCompanies = topCompaniesList
                )
        }

        override fun getTechDetails(techName: String): TechDetailsPageDto {
                val formattedTechName = techName.replaceFirstChar { it.uppercase() }

                val senioritySql =
                        """
            SELECT seniorityLevel as name, COUNT(*) as value
            FROM `$datasetName.$jobsTableName`, UNNEST(technologies) as t
            WHERE LOWER(t) = LOWER(@techName)
            AND DATE(postedDate) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
            GROUP BY name
            ORDER BY value DESC
        """.trimIndent()

                val companiesSql =
                        """
            SELECT c.companyId as id, MAX(c.name) as name, MAX(c.logoUrl) as logo, COUNT(*) as activeRoles
            FROM `$datasetName.$jobsTableName` j
            JOIN `$datasetName.$companiesTableName` c ON j.companyId = c.companyId,
            UNNEST(j.technologies) as t
            WHERE LOWER(t) = LOWER(@techName)
            AND DATE(j.postedDate) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
            GROUP BY c.companyId
            ORDER BY activeRoles DESC
        """.trimIndent()

                val senResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(senioritySql)
                                        .addNamedParameter(
                                                "techName",
                                                QueryParameterValue.string(techName)
                                        )
                                        .build()
                        )
                val compResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(companiesSql)
                                        .addNamedParameter(
                                                "techName",
                                                QueryParameterValue.string(techName)
                                        )
                                        .build()
                        )

                val seniorityDistribution =
                        senResult.values.map {
                                SeniorityDistributionDto(
                                        it.get("name").stringValue,
                                        it.get("value").longValue.toInt()
                                )
                        }

                val companies =
                        compResult.values.map {
                                CompanyLeaderboardDto(
                                        id = it.get("id").stringValue,
                                        name = it.get("name").stringValue,
                                        logo =
                                                if (it.get("logo").isNull) ""
                                                else it.get("logo").stringValue,
                                        activeRoles = it.get("activeRoles").longValue.toInt()
                                )
                        }

                return TechDetailsPageDto(formattedTechName, seniorityDistribution, companies)
        }

        override fun getCompanyProfile(companyId: String): CompanyProfilePageDto {
                val detailsSql =
                        """
            SELECT name, logoUrl, website, employeesCount, industries, description, technologies, hiringLocations
            FROM `$datasetName.$companiesTableName`
            WHERE companyId = @companyId
            LIMIT 1
        """.trimIndent()

                val jobsSql =
                        """
            SELECT jobIds, applyUrls, locations, title, salaryMin, salaryMax, postedDate, technologies, benefits
            FROM `$datasetName.$jobsTableName`
            WHERE companyId = @companyId
            ORDER BY postedDate DESC
        """.trimIndent()

                val aggSql =
                        """
            SELECT MAX(workModel) as topModel
            FROM (
                SELECT workModel, COUNT(*) as c FROM `$datasetName.$jobsTableName` WHERE companyId = @companyId GROUP BY workModel ORDER BY c DESC LIMIT 1
            ) wm
        """.trimIndent()

                val detResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(detailsSql)
                                        .addNamedParameter(
                                                "companyId",
                                                QueryParameterValue.string(companyId)
                                        )
                                        .build()
                        )
                val jobsResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(jobsSql)
                                        .addNamedParameter(
                                                "companyId",
                                                QueryParameterValue.string(companyId)
                                        )
                                        .build()
                        )
                val aggResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(aggSql)
                                        .addNamedParameter(
                                                "companyId",
                                                QueryParameterValue.string(companyId)
                                        )
                                        .build()
                        )

                val detRow = detResult.values.firstOrNull()
                val name = detRow?.get("name")?.stringValue ?: "Unknown Company"
                val logo =
                        if (detRow?.get("logoUrl")?.isNull == false)
                                detRow.get("logoUrl").stringValue
                        else ""
                val website =
                        if (detRow?.get("website")?.isNull == false)
                                detRow.get("website").stringValue
                        else ""
                val emps =
                        if (detRow?.get("employeesCount")?.isNull == false)
                                detRow.get("employeesCount").longValue.toInt()
                        else 0
                val ind =
                        if (detRow?.get("industries")?.isNull == false)
                                detRow.get("industries").stringValue
                        else ""
                val desc =
                        if (detRow?.get("description")?.isNull == false)
                                detRow.get("description").stringValue
                        else ""

                val details = CompanyDetailsDto(companyId, name, logo, website, emps, ind, desc)

                val roles =
                        jobsResult.values.map { r ->
                                val techList =
                                        if (r.get("technologies").isNull) emptyList<String>()
                                        else
                                                r.get("technologies").repeatedValue.map {
                                                        it.stringValue
                                                }
                                val locationList =
                                        if (r.get("locations").isNull) emptyList<String>()
                                        else r.get("locations").repeatedValue.map { it.stringValue }
                                val jobIdList =
                                        if (r.get("jobIds").isNull) emptyList<String>()
                                        else r.get("jobIds").repeatedValue.map { it.stringValue }
                                val applyUrlList =
                                        if (r.get("applyUrls").isNull) emptyList<String?>()
                                        else
                                                r.get("applyUrls").repeatedValue.map {
                                                        if (it.isNull) null else it.stringValue
                                                }
                                JobRoleDto(
                                        id = jobIdList.firstOrNull() ?: "",
                                        title = r.get("title").stringValue,
                                        companyId = companyId,
                                        companyName = name,
                                        locations = locationList,
                                        jobIds = jobIdList,
                                        applyUrls = applyUrlList,
                                        salaryMin =
                                                if (r.get("salaryMin").isNull) null
                                                else r.get("salaryMin").longValue.toInt(),
                                        salaryMax =
                                                if (r.get("salaryMax").isNull) null
                                                else r.get("salaryMax").longValue.toInt(),
                                        postedDate =
                                                if (r.get("postedDate").isNull) ""
                                                else r.get("postedDate").stringValue,
                                        technologies = techList
                                )
                        }

                val allTechs =
                        if (detRow?.get("technologies")?.isNull == false)
                                detRow.get("technologies").repeatedValue.map { it.stringValue }
                        else emptyList()

                val hiringLocations =
                        if (detRow?.get("hiringLocations")?.isNull == false)
                                detRow.get("hiringLocations").repeatedValue.map { it.stringValue }
                        else emptyList()

                val aggRow = aggResult.values.firstOrNull()
                val topModel =
                        if (aggRow?.get("topModel")?.isNull == false)
                                aggRow.get("topModel").stringValue
                        else "Hybrid Friendly"

                val allBenefits =
                        jobsResult
                                .values
                                .mapNotNull { r ->
                                        if (r.get("benefits").isNull) null
                                        else r.get("benefits").repeatedValue.map { it.stringValue }
                                }
                                .flatten()
                                .groupingBy { it }
                                .eachCount()
                                .entries
                                .sortedByDescending { it.value }
                                .map { it.key }
                                .take(5)

                val insights = CompanyInsightsDto(topModel, hiringLocations, allBenefits)

                return CompanyProfilePageDto(details, allTechs, insights, roles)
        }

        override fun deleteAllCompanies() {
                log.info("GCP: Deleting all rows from $companiesTableName")
                val query = "DELETE FROM $datasetName.$companiesTableName WHERE true"
                val queryConfig = QueryJobConfiguration.newBuilder(query).build()
                try {
                        bigQuery.query(queryConfig)
                        log.info("GCP: Successfully deleted all rows from $companiesTableName")
                } catch (e: Exception) {
                        log.error(
                                "GCP: Failed to delete rows from $companiesTableName: ${e.message}",
                                e
                        )
                        throw e
                }
        }

        override fun saveCompanies(companies: List<CompanyRecord>) {
                if (companies.isEmpty()) return

                ensureAllTables()

                log.info(
                        "GCP: Streaming ${companies.size} companies to BigQuery table: $companiesTableName"
                )
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        companiesTableName,
                                        companies.map { it.toMap() }.byteInputStream()
                                )
                                .get(
                                        60,
                                        TimeUnit.SECONDS
                                ) // Block until write is acknowledged — see saveRawIngestions for
                        // rationale
                        log.info("GCP: Successfully inserted companies into BigQuery.")
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert companies into BigQuery: ${e.message}", e)
                        throw e
                }
        }

        override fun getSearchSuggestions(): SearchSuggestionsResponse {
                log.info("GCP: Querying search suggestions from BigQuery")
                val query =
                        """
            SELECT 'COMPANY' as type, companyId as id, name FROM `$datasetName.$companiesTableName`
            UNION DISTINCT
            SELECT DISTINCT 'TECHNOLOGY' as type, LOWER(t) as id, t as name FROM `$datasetName.$jobsTableName`, UNNEST(technologies) as t
        """.trimIndent()

                try {
                        val result = bigQuery.query(QueryJobConfiguration.newBuilder(query).build())
                        val suggestions =
                                result.values.map { row ->
                                        val type = row.get("type").stringValue
                                        val id = row.get("id").stringValue
                                        val name = row.get("name").stringValue
                                        SearchSuggestionDto(type, id, name)
                                }
                        return SearchSuggestionsResponse(suggestions.toList())
                } catch (e: Exception) {
                        log.error("GCP: Failed to fetch search suggestions: ${e.message}", e)
                        return SearchSuggestionsResponse(emptyList())
                }
        }

        override fun saveSearchMiss(term: String) {
                ensureAllTables()
                log.info("GCP: Saving search miss to BigQuery: $term")
                val record = mapOf("term" to term, "timestamp" to Instant.now().toString())
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        searchMissesTableName,
                                        listOf(record).byteInputStream()
                                )
                                .get(
                                        30,
                                        TimeUnit.SECONDS
                                ) // Block until write is acknowledged — see saveRawIngestions for
                        // rationale
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert search miss: ${e.message}", e)
                }
        }

        override fun saveFeedback(context: String?, message: String) {
                ensureAllTables()
                log.info("GCP: Saving user feedback to BigQuery")
                val record =
                        mapOf(
                                "context" to context,
                                "message" to message,
                                "timestamp" to Instant.now().toString()
                        )
                try {
                        bigQueryTemplate
                                .writeJsonStream(
                                        feedbackTableName,
                                        listOf(record).byteInputStream()
                                )
                                .get(
                                        30,
                                        TimeUnit.SECONDS
                                ) // Block until write is acknowledged — see saveRawIngestions for
                        // rationale
                } catch (e: Exception) {
                        log.error("GCP: Failed to insert feedback: ${e.message}", e)
                }
        }

        private fun RawIngestionRecord.toMap(): Map<String, Any?> {
                return mapOf(
                        "id" to this.id,
                        "source" to this.source,
                        "ingestedAt" to this.ingestedAt.toString(),
                        "rawPayload" to this.rawPayload
                )
        }

        private fun JobRecord.toMap(): Map<String, Any?> {
                return mapOf(
                        "jobIds" to this.jobIds,
                        "applyUrls" to this.applyUrls,
                        "locations" to this.locations,
                        "companyId" to this.companyId,
                        "companyName" to this.companyName,
                        "source" to this.source,
                        "country" to this.country,
                        "title" to this.title,
                        "seniorityLevel" to this.seniorityLevel,
                        "technologies" to this.technologies,
                        "salaryMin" to this.salaryMin,
                        "salaryMax" to this.salaryMax,
                        "postedDate" to this.postedDate?.toString(),
                        "benefits" to this.benefits,
                        "employmentType" to this.employmentType,
                        "workModel" to this.workModel,
                        "jobFunction" to this.jobFunction,
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
                        "technologies" to this.technologies,
                        "hiringLocations" to this.hiringLocations,
                        "ingestedAt" to this.ingestedAt.toString()
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
}
