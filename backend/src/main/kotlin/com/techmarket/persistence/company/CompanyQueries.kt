package com.techmarket.persistence.company

import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields

object CompanyQueries {
    fun getDetailsSql(datasetName: String, companiesTableName: String) =
            """
        SELECT ${CompanyFields.NAME}, ${CompanyFields.LOGO_URL}, ${CompanyFields.WEBSITE}, ${CompanyFields.EMPLOYEES_COUNT}, ${CompanyFields.INDUSTRIES}, ${CompanyFields.DESCRIPTION}, ${CompanyFields.TECHNOLOGIES}, ${CompanyFields.HIRING_LOCATIONS}
        FROM `$datasetName.$companiesTableName`
        WHERE ${CompanyFields.COMPANY_ID} = @companyId
        LIMIT 1
    """.trimIndent()

    fun getJobsSql(datasetName: String, jobsTableName: String) =
            """
        SELECT ${JobFields.JOB_IDS}, ${JobFields.APPLY_URLS}, ${JobFields.LOCATIONS}, ${JobFields.TITLE}, ${JobFields.SALARY_MIN}, ${JobFields.SALARY_MAX}, ${JobFields.POSTED_DATE}, ${JobFields.TECHNOLOGIES}, ${JobFields.BENEFITS}
        FROM `$datasetName.$jobsTableName`
        WHERE ${JobFields.COMPANY_ID} = @companyId
        ORDER BY ${JobFields.POSTED_DATE} DESC
    """.trimIndent()

    fun getAggSql(datasetName: String, jobsTableName: String) =
            """
        SELECT MAX(${JobFields.WORK_MODEL}) as topModel
        FROM (
            SELECT ${JobFields.WORK_MODEL}, COUNT(*) as c FROM `$datasetName.$jobsTableName` WHERE ${JobFields.COMPANY_ID} = @companyId GROUP BY ${JobFields.WORK_MODEL} ORDER BY c DESC LIMIT 1
        ) wm
    """.trimIndent()
}
