package com.techmarket.persistence.analytics

import com.google.cloud.bigquery.*
import com.techmarket.model.TechCategory
import com.techmarket.dto.*
import com.techmarket.persistence.*
import com.techmarket.util.TechFormatter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class InsightsBigQueryRepositoryImpl(
    private val bigQuery: BigQuery,
    @Value("${'$'}{spring.cloud.gcp.bigquery.dataset-name:techmarket}")
    private val datasetName: String
) : InsightsBigQueryRepository {

    override fun getTechnologiesByCategory(
        category: TechCategory,
        country: String?
    ): List<TechnologyDto> {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        if (techKeys.isEmpty()) return emptyList()

        val query = HubQueries.getTechnologiesByCategorySql(datasetName, country != null).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        return executeQuery(queryConfig) { row ->
            val avgSalary = try {
                val value = row.get(HubFields.AVG_SALARY_MAX)
                if (value.isNull) null else value.doubleValue / 100.0
            } catch (e: Exception) {
                null
            }

            TechnologyDto(
                name = TechFormatter.format(row.getString(HubFields.TECHNOLOGY)),
                jobCount = row.getLongOrNull(HubFields.JOB_COUNT)?.toInt() ?: 0,
                companyCount = row.getLongOrNull(HubFields.COMPANY_COUNT)?.toInt() ?: 0,
                avgSalary = avgSalary
            )
        }
    }

    override fun getJobsByCategory(
        category: TechCategory,
        country: String?
    ): List<JobRoleDto> {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        if (techKeys.isEmpty()) return emptyList()

        val query = HubQueries.getJobsByCategorySql(datasetName, country != null).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        return executeQuery(queryConfig) { row ->
            JobRoleDto(
                id = row.getString(JobFields.JOB_ID),
                title = row.getString(JobFields.TITLE),
                companyId = row.getString(JobFields.COMPANY_ID),
                companyName = row.getString(JobFields.COMPANY_NAME),
                location = row.getString(JobFields.CITY),
                country = row.getString(JobFields.COUNTRY),
                salaryMin = (row.getLongOrNull(HubFields.SALARY_MIN) ?: 0L) / 100,
                salaryMax = (row.getLongOrNull(HubFields.SALARY_MAX) ?: 0L) / 100,
                postedDate = row.getTimestamp(JobFields.POSTED_DATE),
                url = row.getStringOrDefault(HubFields.JOB_URL, "")
            )
        }
    }

    override fun getCompaniesByCategory(
        category: TechCategory,
        country: String?
    ): List<CompanyDto> {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        if (techKeys.isEmpty()) return emptyList()

        val query = HubQueries.getCompaniesByCategorySql(datasetName, country != null).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        return executeQuery(queryConfig) { row ->
            val techField = try { row.get(JobFields.TECHNOLOGIES) } catch(e: Exception) { null }
            val technologies = techField?.repeatedValue?.map { 
                TechFormatter.format(it.stringValue) 
            } ?: emptyList()

            CompanyDto(
                id = row.getString(JobFields.COMPANY_ID),
                name = row.getString(JobFields.COMPANY_NAME),
                jobCount = row.getLongOrNull(HubFields.JOB_COUNT)?.toInt() ?: 0,
                technologies = technologies
            )
        }
    }

    override fun getCategoryTrends(
        category: TechCategory,
        country: String?
    ): CategoryTrendsDto {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        if (techKeys.isEmpty()) return CategoryTrendsDto(0, 0, 0.0, 0.0, 0, emptyList())

        val query = HubQueries.getTrendsSql(datasetName, country != null).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        val rowList = executeQuery(queryConfig) { it }
        val row = rowList.firstOrNull()

        return if (row != null) {
            val currentJobs = row.getLongOrNull(HubFields.CURRENT_MONTH_JOBS)?.toInt() ?: 0
            val prevJobs = row.getLongOrNull(HubFields.PREV_MONTH_JOBS)?.toInt() ?: 0
            val growthRate = if (prevJobs > 0) ((currentJobs - prevJobs).toDouble() / prevJobs) * 100 else 0.0
            
            CategoryTrendsDto(
                totalJobs = row.getLongOrNull(HubFields.TOTAL_JOBS)?.toInt() ?: 0,
                totalCompanies = row.getLongOrNull(HubFields.TOTAL_COMPANIES)?.toInt() ?: 0,
                growthRate = growthRate,
                marketShare = calculateMarketShare(category, country),
                last6MonthsJobs = row.getLongOrNull(HubFields.LAST_6_MONTHS_JOBS)?.toInt() ?: 0,
                monthlyData = getMonthlyTrendData(category, country)
            )
        } else {
            CategoryTrendsDto(0, 0, 0.0, 0.0, 0, emptyList())
        }
    }

    private fun getMonthlyTrendData(category: TechCategory, country: String?): List<MonthlyTrendDto> {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        
        val query = HubQueries.getMonthlyTrendsSql(datasetName, country != null).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        return executeQuery(queryConfig) { row ->
            MonthlyTrendDto(
                month = row.getString(HubFields.MONTH),
                jobCount = row.getLongOrNull(HubFields.JOB_COUNT)?.toInt() ?: 0,
                companyCount = row.getLongOrNull(HubFields.COMPANY_COUNT)?.toInt() ?: 0
            )
        }
    }

    override fun countJobsByCategory(category: TechCategory, country: String?): Int {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        if (techKeys.isEmpty()) return 0

        val query = HubQueries.getCountJobsSql(datasetName, country != null).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        return executeQuery(queryConfig) { row -> row.getLongOrNull(HubFields.JOB_COUNT)?.toInt() ?: 0 }.firstOrNull() ?: 0
    }

    override fun countCompaniesByCategory(category: TechCategory, country: String?): Int {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        if (techKeys.isEmpty()) return 0

        val query = HubQueries.getCountCompaniesSql(datasetName, country != null).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        return executeQuery(queryConfig) { row -> row.getLongOrNull(HubFields.COMPANY_COUNT)?.toInt() ?: 0 }.firstOrNull() ?: 0
    }
    
    override fun countTechnologiesByCategory(category: TechCategory): Int {
        return TechFormatter.countDistinctTechsByCategory(category)
    }

    override fun getAllCategorySummaries(country: String?): List<DomainSummaryDto> {
        val categoriesWithKeys = TechCategory.entries.filter { TechFormatter.getTechKeysForCategory(it).isNotEmpty() }
        if (categoriesWithKeys.isEmpty()) return TechCategory.entries.map { fallback(it) }

        val query = HubQueries.getAllCategorySummariesSql(datasetName, country, categoriesWithKeys).sql

        val queryConfig = QueryJobConfiguration.newBuilder(query)
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
                categoriesWithKeys.forEach { category ->
                    val keys = TechFormatter.getTechKeysForCategory(category)
                    addNamedParameter("keys_${category.name}", 
                        QueryParameterValue.array(keys.toTypedArray(), StandardSQLTypeName.STRING))
                }
            }
            .build()

        val results = executeQuery(queryConfig) { row ->
            val catName = row.getString(HubFields.CATEGORY_NAME)
            val category = TechCategory.valueOf(catName)
            val current = row.getLongOrNull(HubFields.CURRENT_JOBS) ?: 0L
            val prev = row.getLongOrNull(HubFields.PREV_JOBS) ?: 0L
            val globalTotal = row.getLongOrNull(HubFields.GLOBAL_TOTAL_JOBS) ?: 1L
            val jobCount = row.getLongOrNull(HubFields.JOB_COUNT) ?: 0L
            
            DomainSummaryDto(
                category = CategoryDto(category.slug, category.displayName, category.description),
                jobCount = jobCount.toInt(),
                companyCount = row.getLongOrNull(HubFields.COMPANY_COUNT)?.toInt() ?: 0,
                techCount = TechFormatter.countDistinctTechsByCategory(category),
                growthRate = if (prev > 0) ((current - prev).toDouble() / prev * 100) else 0.0,
                marketShare = (current.toDouble() / globalTotal * 100)
            )
        }

        return TechCategory.entries.map { category ->
            results.find { it.category.slug == category.slug }
                ?: fallback(category)
        }
    }

    private fun fallback(category: TechCategory) = DomainSummaryDto(
        category = CategoryDto(category.slug, category.displayName, category.description),
        jobCount = 0,
        companyCount = 0,
        techCount = TechFormatter.countDistinctTechsByCategory(category),
        growthRate = 0.0,
        marketShare = 0.0
    )

    private fun calculateMarketShare(category: TechCategory, country: String?): Double {
        val techKeys = TechFormatter.getTechKeysForCategory(category)
        if (techKeys.isEmpty()) return 0.0
        
        val categoryQuery = HubQueries.getMarketShareCategoryCountSql(datasetName, country != null).sql
        val totalQuery = HubQueries.getMarketShareTotalCountSql(datasetName, country != null).sql

        val categoryQueryConfig = QueryJobConfiguration.newBuilder(categoryQuery)
            .addNamedParameter("technologies", 
                QueryParameterValue.array(techKeys.toTypedArray(), StandardSQLTypeName.STRING))
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        val totalQueryConfig = QueryJobConfiguration.newBuilder(totalQuery)
            .apply {
                if (country != null) {
                    addNamedParameter("country", QueryParameterValue.string(country.lowercase()))
                }
            }
            .build()

        val categoryJobs = executeQuery(categoryQueryConfig) { row -> row.getLongOrNull(HubFields.CATEGORY_COUNT) ?: 0L }.firstOrNull() ?: 0L
        val totalJobs = executeQuery(totalQueryConfig) { row -> row.getLongOrNull(HubFields.TOTAL_COUNT) ?: 0L }.firstOrNull() ?: 0L

        return if (totalJobs > 0) (categoryJobs.toDouble() / totalJobs) * 100 else 0.0
    }

    private inline fun <T> executeQuery(
        queryConfig: QueryJobConfiguration,
        mapper: (FieldValueList) -> T
    ): List<T> {
        val result = bigQuery.query(queryConfig)
        return result.iterateAll().map { mapper(it) }
    }
}
