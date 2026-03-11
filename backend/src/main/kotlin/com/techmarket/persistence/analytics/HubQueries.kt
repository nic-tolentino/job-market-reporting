package com.techmarket.persistence.analytics

import com.techmarket.model.TechCategory
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.HubFields
import com.techmarket.persistence.SalaryFields

/**
 * Centrally managed SQL queries for Technology Domain Hubs.
 * Following the project pattern of extracting queries for SQL safety testing.
 */
object HubQueries {

    data class HubQuery(
        val sql: String,
        val requiredFields: List<String> = emptyList()
    )

    fun getTechnologiesByCategorySql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT
                t as ${HubFields.TECHNOLOGY},
                COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.JOB_COUNT},
                COUNT(DISTINCT ${JobFields.COMPANY_ID}) as ${HubFields.COMPANY_COUNT},
                AVG(${JobFields.SALARY_MAX}.${SalaryFields.AMOUNT}) as ${HubFields.AVG_SALARY_MAX}
            FROM `$datasetName.${BigQueryTables.JOBS}` j,
            UNNEST(j.${JobFields.TECHNOLOGIES}) as t
            WHERE LOWER(t) IN UNNEST(@technologies)
            ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
            GROUP BY t
            ORDER BY ${HubFields.JOB_COUNT} DESC
        """.trimIndent(),
        requiredFields = listOf(HubFields.TECHNOLOGY, HubFields.JOB_COUNT, HubFields.COMPANY_COUNT, HubFields.AVG_SALARY_MAX)
    )

    fun getJobsByCategorySql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT
                j.${JobFields.JOB_ID},
                j.${JobFields.TITLE},
                j.${JobFields.COMPANY_ID},
                j.${JobFields.COMPANY_NAME},
                j.${JobFields.CITY},
                j.${JobFields.COUNTRY},
                j.${JobFields.SALARY_MIN}.${SalaryFields.AMOUNT} as ${HubFields.SALARY_MIN},
                j.${JobFields.SALARY_MAX}.${SalaryFields.AMOUNT} as ${HubFields.SALARY_MAX},
                j.${JobFields.POSTED_DATE},
                j.${JobFields.PLATFORM_LINKS}[SAFE_OFFSET(0)] as ${HubFields.JOB_URL}
            FROM `$datasetName.${BigQueryTables.JOBS}` j
            WHERE EXISTS(SELECT 1 FROM UNNEST(${JobFields.TECHNOLOGIES}) t WHERE LOWER(t) IN UNNEST(@technologies))
            ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
            AND ARRAY_LENGTH(j.${JobFields.PLATFORM_LINKS}) > 0
            ORDER BY j.${JobFields.POSTED_DATE} DESC
            LIMIT 20
        """.trimIndent(),
        requiredFields = listOf(
            JobFields.JOB_ID, JobFields.TITLE, JobFields.COMPANY_ID, JobFields.COMPANY_NAME,
            JobFields.CITY, JobFields.COUNTRY, HubFields.SALARY_MIN, HubFields.SALARY_MAX, JobFields.POSTED_DATE, HubFields.JOB_URL
        )
    )

    fun getCompaniesByCategorySql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT
                ${JobFields.COMPANY_ID},
                ${JobFields.COMPANY_NAME},
                COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.JOB_COUNT},
                ARRAY_AGG(DISTINCT t LIMIT 10) as ${JobFields.TECHNOLOGIES}
            FROM `$datasetName.${BigQueryTables.JOBS}` j,
            UNNEST(j.${JobFields.TECHNOLOGIES}) as t
            WHERE LOWER(t) IN UNNEST(@technologies)
            ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
            GROUP BY ${JobFields.COMPANY_ID}, ${JobFields.COMPANY_NAME}
            ORDER BY ${HubFields.JOB_COUNT} DESC
            LIMIT 50
        """.trimIndent(),
        requiredFields = listOf(JobFields.COMPANY_ID, JobFields.COMPANY_NAME, HubFields.JOB_COUNT, JobFields.TECHNOLOGIES)
    )

    fun getTrendsSql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT
                COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.TOTAL_JOBS},
                COUNT(DISTINCT ${JobFields.COMPANY_ID}) as ${HubFields.TOTAL_COMPANIES},
                COUNTIF(DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)) as ${HubFields.CURRENT_MONTH_JOBS},
                COUNTIF(DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 60 DAY) AND DATE(${JobFields.POSTED_DATE}) < DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)) as ${HubFields.PREV_MONTH_JOBS},
                COUNTIF(DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 180 DAY)) as ${HubFields.LAST_6_MONTHS_JOBS}
            FROM `$datasetName.${BigQueryTables.JOBS}`
            WHERE EXISTS(SELECT 1 FROM UNNEST(${JobFields.TECHNOLOGIES}) t WHERE LOWER(t) IN UNNEST(@technologies))
            ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
        """.trimIndent(),
        requiredFields = listOf(HubFields.TOTAL_JOBS, HubFields.TOTAL_COMPANIES, HubFields.CURRENT_MONTH_JOBS, HubFields.PREV_MONTH_JOBS, HubFields.LAST_6_MONTHS_JOBS)
    )

    fun getMonthlyTrendsSql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT
                FORMAT_DATE('%Y-%m', DATE(${JobFields.POSTED_DATE})) as ${HubFields.MONTH},
                COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.JOB_COUNT},
                COUNT(DISTINCT ${JobFields.COMPANY_ID}) as ${HubFields.COMPANY_COUNT}
            FROM `$datasetName.${BigQueryTables.JOBS}`
            WHERE EXISTS(SELECT 1 FROM UNNEST(${JobFields.TECHNOLOGIES}) t WHERE LOWER(t) IN UNNEST(@technologies))
            ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
            AND DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 6 MONTH)
            GROUP BY ${HubFields.MONTH}
            ORDER BY ${HubFields.MONTH} ASC
        """.trimIndent(),
        requiredFields = listOf(HubFields.MONTH, HubFields.JOB_COUNT, HubFields.COMPANY_COUNT)
    )

    fun getCountJobsSql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.JOB_COUNT}
            FROM `$datasetName.${BigQueryTables.JOBS}`
            WHERE EXISTS(SELECT 1 FROM UNNEST(${JobFields.TECHNOLOGIES}) t WHERE LOWER(t) IN UNNEST(@technologies))
            ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
        """.trimIndent(),
        requiredFields = listOf(HubFields.JOB_COUNT)
    )

    fun getCountCompaniesSql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT COUNT(DISTINCT ${JobFields.COMPANY_ID}) as ${HubFields.COMPANY_COUNT}
            FROM `$datasetName.${BigQueryTables.JOBS}`
            WHERE EXISTS(SELECT 1 FROM UNNEST(${JobFields.TECHNOLOGIES}) t WHERE LOWER(t) IN UNNEST(@technologies))
            ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
        """.trimIndent(),
        requiredFields = listOf(HubFields.COMPANY_COUNT)
    )

    /**
     * Specialized batch query for fetching ALL domain summaries in a single BigQuery pass.
     * Uses UNION ALL to aggregate metrics across all technology categories.
     */
    fun getAllCategorySummariesSql(datasetName: String, country: String?, categories: List<TechCategory>) : HubQuery {
        val unionParts = categories.map { category ->
            """
            SELECT 
                '${category.name}' as ${HubFields.CATEGORY_NAME},
                ${JobFields.JOB_ID},
                ${JobFields.COMPANY_ID},
                ${JobFields.POSTED_DATE}
            FROM `$datasetName.${BigQueryTables.JOBS}`
            WHERE EXISTS(SELECT 1 FROM UNNEST(${JobFields.TECHNOLOGIES}) t WHERE LOWER(t) IN UNNEST(@keys_${category.name}))
            ${if (country != null) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
            """.trimIndent()
        }

        val mainQuery = """
            WITH category_raw AS (
                ${unionParts.joinToString("\nUNION ALL\n")}
            ),
            total_jobs AS (
                SELECT COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.TOTAL_COUNT}
                FROM `$datasetName.${BigQueryTables.JOBS}`
                WHERE ${if (country != null) "LOWER(${JobFields.COUNTRY}) = @country" else "1=1"}
            )
            SELECT 
                ${HubFields.CATEGORY_NAME},
                COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.JOB_COUNT},
                COUNT(DISTINCT ${JobFields.COMPANY_ID}) as ${HubFields.COMPANY_COUNT},
                COUNTIF(DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)) as ${HubFields.CURRENT_JOBS},
                COUNTIF(DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 60 DAY) AND DATE(${JobFields.POSTED_DATE}) < DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)) as ${HubFields.PREV_JOBS},
                ANY_VALUE((SELECT ${HubFields.TOTAL_COUNT} FROM total_jobs)) as ${HubFields.GLOBAL_TOTAL_JOBS}
            FROM category_raw
            GROUP BY ${HubFields.CATEGORY_NAME}
        """.trimIndent()

        return HubQuery(
            sql = mainQuery,
            requiredFields = listOf(HubFields.CATEGORY_NAME, HubFields.JOB_COUNT, HubFields.COMPANY_COUNT, HubFields.CURRENT_JOBS, HubFields.PREV_JOBS, HubFields.GLOBAL_TOTAL_JOBS)
        )
    }

    fun getMarketShareCategoryCountSql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.CATEGORY_COUNT}
            FROM `$datasetName.${BigQueryTables.JOBS}`
            WHERE EXISTS(
                SELECT 1 FROM UNNEST(${JobFields.TECHNOLOGIES}) t 
                WHERE LOWER(t) IN UNNEST(@technologies)
            ) ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
        """.trimIndent(),
        requiredFields = listOf(HubFields.CATEGORY_COUNT)
    )

    fun getMarketShareTotalCountSql(datasetName: String, hasCountry: Boolean) = HubQuery(
        sql = """
            SELECT COUNT(DISTINCT ${JobFields.JOB_ID}) as ${HubFields.TOTAL_COUNT}
            FROM `$datasetName.${BigQueryTables.JOBS}`
            WHERE 1=1 ${if (hasCountry) "AND LOWER(${JobFields.COUNTRY}) = @country" else ""}
        """.trimIndent(),
        requiredFields = listOf(HubFields.TOTAL_COUNT)
    )
}
