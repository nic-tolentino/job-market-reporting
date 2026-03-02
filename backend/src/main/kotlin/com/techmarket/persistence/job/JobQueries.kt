package com.techmarket.persistence.job

import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields

object JobQueries {
    fun getDetailsSql(datasetName: String, jobsTableName: String, companiesTableName: String) =
            """
        SELECT j.*,
               c.${CompanyFields.NAME} as comp_name, 
               c.${CompanyFields.LOGO_URL} as comp_logo, 
               c.${CompanyFields.DESCRIPTION} as comp_desc,
               c.${CompanyFields.WEBSITE} as comp_web, 
               c.${CompanyFields.HIRING_LOCATIONS} as comp_hiringLocations
        FROM `$datasetName.$jobsTableName` j
        JOIN `$datasetName.$companiesTableName` c ON j.${JobFields.COMPANY_ID} = c.${CompanyFields.COMPANY_ID}
        WHERE j.${JobFields.JOB_ID} = @jobId OR @jobId IN UNNEST(j.${JobFields.JOB_IDS})
        LIMIT 1
    """.trimIndent()

    fun getSimilarSql(datasetName: String, jobsTableName: String, techList: List<String>): String {
        return if (techList.isEmpty()) {
            """
            SELECT ${JobFields.JOB_IDS}, ${JobFields.APPLY_URLS}, ${JobFields.PLATFORM_LINKS}, ${JobFields.LOCATIONS}, ${JobFields.TITLE}, ${JobFields.COMPANY_ID}, ${JobFields.COMPANY_NAME}, ${JobFields.SALARY_MIN}, ${JobFields.SALARY_MAX}, ${JobFields.POSTED_DATE}, ${JobFields.TECHNOLOGIES}, ${JobFields.CITY}, ${JobFields.STATE_REGION}, ${JobFields.SENIORITY_LEVEL}
            FROM `$datasetName.$jobsTableName`
            WHERE ${JobFields.SENIORITY_LEVEL} = @seniority
              AND @jobId NOT IN UNNEST(${JobFields.JOB_IDS})
            ORDER BY ${JobFields.POSTED_DATE} DESC
            LIMIT 3
            """.trimIndent()
        } else {
            val techArrayString = techList.joinToString("','", "'", "'")
            """
            SELECT DISTINCT j.${JobFields.JOB_IDS}, j.${JobFields.APPLY_URLS}, j.${JobFields.PLATFORM_LINKS}, j.${JobFields.LOCATIONS}, j.${JobFields.TITLE}, j.${JobFields.COMPANY_ID}, j.${JobFields.COMPANY_NAME}, j.${JobFields.SALARY_MIN}, j.${JobFields.SALARY_MAX}, j.${JobFields.POSTED_DATE}, j.${JobFields.TECHNOLOGIES}, j.${JobFields.CITY}, j.${JobFields.STATE_REGION}, j.${JobFields.SENIORITY_LEVEL}
            FROM `$datasetName.$jobsTableName` j, UNNEST(j.${JobFields.TECHNOLOGIES}) t
            WHERE j.${JobFields.SENIORITY_LEVEL} = @seniority
              AND @jobId NOT IN UNNEST(j.${JobFields.JOB_IDS})
              AND t IN ($techArrayString)
            ORDER BY j.${JobFields.POSTED_DATE} DESC
            LIMIT 3
            """.trimIndent()
        }
    }
}
