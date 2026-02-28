package com.techmarket.persistence.analytics

import com.techmarket.persistence.AnalyticsFields
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields

object AnalyticsQueries {
    fun getStatsSql(datasetName: String, jobsTableName: String) =
            """
        SELECT 
            COUNT(*) as totalVacancies,
            IFNULL(SUM(IF(${JobFields.WORK_MODEL} = 'Remote', 1, 0)), 0) as remoteCount,
            IFNULL(SUM(IF(${JobFields.WORK_MODEL} = 'Hybrid', 1, 0)), 0) as hybridCount
        FROM `$datasetName.$jobsTableName`
        WHERE DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
    """.trimIndent()

    fun getTopTechSql(datasetName: String, jobsTableName: String) =
            """
        SELECT t as name, COUNT(*) as count
        FROM `$datasetName.$jobsTableName`, UNNEST(${JobFields.TECHNOLOGIES}) as t
        WHERE DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
        GROUP BY name
        ORDER BY count DESC
        LIMIT 20
    """.trimIndent()

    fun getTopCompaniesSql(datasetName: String, jobsTableName: String, companiesTableName: String) =
            """
        SELECT c.${CompanyFields.COMPANY_ID} as id, MAX(c.${CompanyFields.NAME}) as name, MAX(c.${CompanyFields.LOGO_URL}) as logo, COUNT(*) as activeRoles
        FROM `$datasetName.$jobsTableName` j
        JOIN `$datasetName.$companiesTableName` c ON j.${JobFields.COMPANY_ID} = c.${CompanyFields.COMPANY_ID}
        WHERE DATE(j.${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
        GROUP BY c.${CompanyFields.COMPANY_ID}
        ORDER BY activeRoles DESC
        LIMIT 20
    """.trimIndent()

    fun getSearchSuggestionsSql(
            datasetName: String,
            companiesTableName: String,
            jobsTableName: String
    ) =
            """
        SELECT 'COMPANY' as type, ${CompanyFields.COMPANY_ID} as id, ${CompanyFields.NAME} as name FROM `$datasetName.$companiesTableName`
        UNION DISTINCT
        SELECT DISTINCT 'TECHNOLOGY' as type, LOWER(t) as id, t as name FROM `$datasetName.$jobsTableName`, UNNEST(${JobFields.TECHNOLOGIES}) as t
    """.trimIndent()

    fun getFeedbackSql(datasetName: String, feedbackTableName: String) =
            """
        SELECT ${AnalyticsFields.CONTEXT}, ${AnalyticsFields.MESSAGE}, ${AnalyticsFields.TIMESTAMP}
        FROM `$datasetName.$feedbackTableName`
        ORDER BY ${AnalyticsFields.TIMESTAMP} DESC
    """.trimIndent()
}
