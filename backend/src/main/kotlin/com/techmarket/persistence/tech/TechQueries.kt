package com.techmarket.persistence.tech

import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields

object TechQueries {
    fun getSenioritySql(datasetName: String, jobsTableName: String) =
            """
        SELECT ${JobFields.SENIORITY_LEVEL} as name, COUNT(*) as value
        FROM `$datasetName.$jobsTableName`, UNNEST(${JobFields.TECHNOLOGIES}) as t
        WHERE LOWER(t) = LOWER(@techName)
        AND DATE(${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
        AND (@country IS NULL OR ${JobFields.COUNTRY} = @country)
        GROUP BY name
        ORDER BY value DESC
    """.trimIndent()

    fun getCompaniesSql(datasetName: String, jobsTableName: String, companiesTableName: String) =
            """
        SELECT c.${CompanyFields.COMPANY_ID} as id, MAX(c.${CompanyFields.NAME}) as name, MAX(c.${CompanyFields.LOGO_URL}) as logo, COUNT(*) as activeRoles
        FROM `$datasetName.$jobsTableName` j
        JOIN `$datasetName.$companiesTableName` c ON j.${JobFields.COMPANY_ID} = c.${CompanyFields.COMPANY_ID},
        UNNEST(j.${JobFields.TECHNOLOGIES}) as t
        WHERE LOWER(t) = LOWER(@techName)
        AND DATE(j.${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
        AND (@country IS NULL OR j.${JobFields.COUNTRY} = @country)
        GROUP BY c.${CompanyFields.COMPANY_ID}
        ORDER BY activeRoles DESC
    """.trimIndent()

    fun getJobsSql(datasetName: String, jobsTableName: String) =
            """
        SELECT ${JobFields.JOB_IDS}, ${JobFields.APPLY_URLS}, ${JobFields.PLATFORM_LINKS}, ${JobFields.LOCATIONS}, ${JobFields.TITLE}, ${JobFields.COMPANY_ID}, ${JobFields.COMPANY_NAME}, ${JobFields.SALARY_MIN}, ${JobFields.SALARY_MAX}, ${JobFields.POSTED_DATE}, ${JobFields.TECHNOLOGIES}, ${JobFields.CITY}, ${JobFields.STATE_REGION}, ${JobFields.SENIORITY_LEVEL}
        FROM `$datasetName.$jobsTableName` j, UNNEST(j.${JobFields.TECHNOLOGIES}) as t
        WHERE LOWER(t) = LOWER(@techName)
        AND DATE(j.${JobFields.POSTED_DATE}) >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)
        AND (@country IS NULL OR j.${JobFields.COUNTRY} = @country)
        ORDER BY j.${JobFields.POSTED_DATE} DESC
    """.trimIndent()
}
