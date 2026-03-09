package com.techmarket.persistence.analytics

import com.techmarket.persistence.AnalyticsFields
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields

object AnalyticsQueries {

    data class AnalyticsQuery(
        val sql: String,
        val requiredFields: List<String>
    )

    fun getStatsSql(datasetName: String, jobsTableName: String): AnalyticsQuery {
        return AnalyticsQuery(
            sql = """
                SELECT 
                    COUNT(*) as ${AnalyticsFields.TOTAL_VACANCIES},
                    IFNULL(SUM(IF(${JobFields.WORK_MODEL} = 'Remote', 1, 0)), 0) as ${AnalyticsFields.REMOTE_COUNT},
                    IFNULL(SUM(IF(${JobFields.WORK_MODEL} = 'Hybrid', 1, 0)), 0) as ${AnalyticsFields.HYBRID_COUNT}
                FROM `$datasetName.$jobsTableName`
                WHERE DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
                AND (@country IS NULL OR ${JobFields.COUNTRY} = @country)
            """.trimIndent(),
            requiredFields = listOf(
                AnalyticsFields.TOTAL_VACANCIES,
                AnalyticsFields.REMOTE_COUNT,
                AnalyticsFields.HYBRID_COUNT
            )
        )
    }

    fun getTopTechSql(datasetName: String, jobsTableName: String): AnalyticsQuery {
        return AnalyticsQuery(
            sql = """
                SELECT t as ${AnalyticsFields.NAME}, COUNT(*) as ${AnalyticsFields.COUNT}
                FROM `$datasetName.$jobsTableName`, UNNEST(${JobFields.TECHNOLOGIES}) as t
                WHERE DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
                AND (@country IS NULL OR ${JobFields.COUNTRY} = @country)
                GROUP BY ${AnalyticsFields.NAME}
                ORDER BY ${AnalyticsFields.COUNT} DESC
                LIMIT 20
            """.trimIndent(),
            requiredFields = listOf(
                AnalyticsFields.NAME,
                AnalyticsFields.COUNT
            )
        )
    }

    fun getTopCompaniesSql(datasetName: String, jobsTableName: String, companiesTableName: String): AnalyticsQuery {
        return AnalyticsQuery(
            sql = """
                SELECT c.${CompanyFields.COMPANY_ID} as ${AnalyticsFields.ID}, 
                       MAX(c.${CompanyFields.NAME}) as ${AnalyticsFields.NAME}, 
                       MAX(c.${CompanyFields.LOGO_URL}) as ${AnalyticsFields.LOGO}, 
                       COUNT(*) as ${AnalyticsFields.ACTIVE_ROLES}
                FROM `$datasetName.$jobsTableName` j
                JOIN `$datasetName.$companiesTableName` c ON j.${JobFields.COMPANY_ID} = c.${CompanyFields.COMPANY_ID}
                WHERE DATE(j.${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
                AND (@country IS NULL OR j.${JobFields.COUNTRY} = @country)
                GROUP BY c.${CompanyFields.COMPANY_ID}
                ORDER BY ${AnalyticsFields.ACTIVE_ROLES} DESC
                LIMIT 20
            """.trimIndent(),
            requiredFields = listOf(
                AnalyticsFields.ID,
                AnalyticsFields.NAME,
                AnalyticsFields.LOGO,
                AnalyticsFields.ACTIVE_ROLES
            )
        )
    }

    fun getSearchSuggestionsSql(
            datasetName: String,
            companiesTableName: String,
            jobsTableName: String
    ): AnalyticsQuery {
        return AnalyticsQuery(
            sql = """
                SELECT 'COMPANY' as ${AnalyticsFields.TYPE}, c.${CompanyFields.COMPANY_ID} as ${AnalyticsFields.ID}, c.${CompanyFields.NAME} as ${AnalyticsFields.NAME} 
                FROM `$datasetName.$companiesTableName` c
                WHERE (@country IS NULL OR EXISTS (
                    SELECT 1 FROM `$datasetName.$jobsTableName` j 
                    WHERE j.${JobFields.COMPANY_ID} = c.${CompanyFields.COMPANY_ID} 
                    AND j.${JobFields.COUNTRY} = @country
                ))
                UNION DISTINCT
                SELECT DISTINCT 'TECHNOLOGY' as ${AnalyticsFields.TYPE}, LOWER(t) as ${AnalyticsFields.ID}, t as ${AnalyticsFields.NAME} FROM `$datasetName.$jobsTableName`, UNNEST(${JobFields.TECHNOLOGIES}) as t
                WHERE (@country IS NULL OR ${JobFields.COUNTRY} = @country)
            """.trimIndent(),
            requiredFields = listOf(
                AnalyticsFields.TYPE,
                AnalyticsFields.ID,
                AnalyticsFields.NAME
            )
        )
    }

    fun getFeedbackSql(datasetName: String, feedbackTableName: String): AnalyticsQuery {
        return AnalyticsQuery(
            sql = """
                SELECT ${AnalyticsFields.CONTEXT}, ${AnalyticsFields.MESSAGE}, ${AnalyticsFields.TIMESTAMP}
                FROM `$datasetName.$feedbackTableName`
                ORDER BY ${AnalyticsFields.TIMESTAMP} DESC
            """.trimIndent(),
            requiredFields = listOf(
                AnalyticsFields.CONTEXT,
                AnalyticsFields.MESSAGE,
                AnalyticsFields.TIMESTAMP
            )
        )
    }
}
