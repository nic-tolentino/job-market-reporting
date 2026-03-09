package com.techmarket.persistence.job

import com.techmarket.persistence.CompanyAliases.DESCRIPTION as COMP_DESC
import com.techmarket.persistence.CompanyAliases.HIRING_LOCATIONS as COMP_HIRING_LOCATIONS
import com.techmarket.persistence.CompanyAliases.HQ_COUNTRY as COMP_HQ_COUNTRY
import com.techmarket.persistence.CompanyAliases.LOGO_URL as COMP_LOGO_URL
import com.techmarket.persistence.CompanyAliases.NAME as COMP_NAME
import com.techmarket.persistence.CompanyAliases.VERIFICATION_LEVEL as COMP_VERIFICATION_LEVEL
import com.techmarket.persistence.CompanyAliases.WEBSITE as COMP_WEBSITE
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.JobFields.APPLY_URLS
import com.techmarket.persistence.JobFields.BENEFITS
import com.techmarket.persistence.JobFields.CITY
import com.techmarket.persistence.JobFields.COMPANY_ID
import com.techmarket.persistence.JobFields.COMPANY_NAME
import com.techmarket.persistence.JobFields.JOB_IDS
import com.techmarket.persistence.JobFields.LOCATIONS
import com.techmarket.persistence.JobFields.PLATFORM_LINKS
import com.techmarket.persistence.JobFields.POSTED_DATE
import com.techmarket.persistence.JobFields.SALARY_MAX
import com.techmarket.persistence.JobFields.SALARY_MIN
import com.techmarket.persistence.JobFields.SENIORITY_LEVEL
import com.techmarket.persistence.JobFields.STATE_REGION
import com.techmarket.persistence.JobFields.TECHNOLOGIES
import com.techmarket.persistence.JobFields.TITLE

object JobQueries {
    
    /**
     * Returns SQL and required fields for job details query.
     * Includes company information via JOIN.
     */
    fun getDetailsSql(datasetName: String, jobsTableName: String, companiesTableName: String): JobDetailsQuery {
        return JobDetailsQuery(
            sql = """
                SELECT 
                    j.${JobFields.JOB_ID},
                    j.${JobFields.JOB_IDS},
                    j.${JobFields.APPLY_URLS},
                    j.${JobFields.PLATFORM_LINKS},
                    j.${JobFields.LOCATIONS},
                    j.${JobFields.TITLE},
                    j.${JobFields.COMPANY_ID},
                    j.${JobFields.COMPANY_NAME},
                    j.${JobFields.SALARY_MIN},
                    j.${JobFields.SALARY_MAX},
                    j.${JobFields.POSTED_DATE},
                    j.${JobFields.TECHNOLOGIES},
                    j.${JobFields.BENEFITS},
                    j.${JobFields.CITY},
                    j.${JobFields.STATE_REGION},
                    j.${JobFields.SENIORITY_LEVEL},
                    j.${JobFields.DESCRIPTION},
                    j.${JobFields.EMPLOYMENT_TYPE},
                    j.${JobFields.WORK_MODEL},
                    j.${JobFields.JOB_FUNCTION},
                    j.${JobFields.SOURCE},
                    j.${JobFields.LAST_SEEN_AT},
                    j.${JobFields.COUNTRY},
                    c.$COMP_NAME as $COMP_NAME,
                    c.$COMP_LOGO_URL as $COMP_LOGO_URL,
                    c.$COMP_DESC as $COMP_DESC,
                    c.$COMP_WEBSITE as $COMP_WEBSITE,
                    c.$COMP_HIRING_LOCATIONS as $COMP_HIRING_LOCATIONS,
                    c.$COMP_HQ_COUNTRY as $COMP_HQ_COUNTRY,
                    c.$COMP_VERIFICATION_LEVEL as $COMP_VERIFICATION_LEVEL
                FROM `$datasetName.$jobsTableName` j
                JOIN `$datasetName.$companiesTableName` c ON j.${JobFields.COMPANY_ID} = c.$COMPANY_ID
                WHERE j.${JobFields.JOB_ID} = @jobId OR @jobId IN UNNEST(j.${JobFields.JOB_IDS})
                LIMIT 1
            """.trimIndent(),
            requiredFields = listOf(
                JobFields.JOB_ID,
                JobFields.JOB_IDS,
                JobFields.APPLY_URLS,
                JobFields.PLATFORM_LINKS,
                JobFields.LOCATIONS,
                JobFields.TITLE,
                JobFields.COMPANY_ID,
                JobFields.COMPANY_NAME,
                JobFields.SALARY_MIN,
                JobFields.SALARY_MAX,
                JobFields.POSTED_DATE,
                JobFields.TECHNOLOGIES,
                JobFields.BENEFITS,
                JobFields.CITY,
                JobFields.STATE_REGION,
                JobFields.SENIORITY_LEVEL,
                JobFields.DESCRIPTION,
                JobFields.EMPLOYMENT_TYPE,
                JobFields.WORK_MODEL,
                JobFields.JOB_FUNCTION,
                JobFields.SOURCE,
                JobFields.LAST_SEEN_AT,
                JobFields.COUNTRY,
                "comp_name",
                "comp_logo",
                "comp_desc",
                "comp_web",
                "comp_hiringLocations",
                "comp_hqCountry",
                "comp_verificationLevel"
            )
        )
    }
    
    /**
     * Returns SQL for similar jobs query.
     */
    fun getSimilarSql(datasetName: String, jobsTableName: String, techList: List<String>): JobSimilarQuery {
        return if (techList.isEmpty()) {
            JobSimilarQuery(
                sql = """
                    SELECT ${JobFields.JOB_IDS}, ${JobFields.APPLY_URLS}, ${JobFields.PLATFORM_LINKS}, ${JobFields.LOCATIONS}, ${JobFields.TITLE}, ${JobFields.COMPANY_ID}, ${JobFields.COMPANY_NAME}, ${JobFields.SALARY_MIN}, ${JobFields.SALARY_MAX}, ${JobFields.POSTED_DATE}, ${JobFields.TECHNOLOGIES}, ${JobFields.CITY}, ${JobFields.STATE_REGION}, ${JobFields.SENIORITY_LEVEL}, ${JobFields.COUNTRY}, ${JobFields.SOURCE}, ${JobFields.LAST_SEEN_AT}
                    FROM `$datasetName.$jobsTableName`
                    WHERE ${JobFields.SENIORITY_LEVEL} = @seniority
                      AND @jobId NOT IN UNNEST(${JobFields.JOB_IDS})
                    ORDER BY ${JobFields.POSTED_DATE} DESC
                    LIMIT 3
                """.trimIndent(),
                requiredFields = listOf(
                    JobFields.JOB_IDS,
                    JobFields.APPLY_URLS,
                    JobFields.PLATFORM_LINKS,
                    JobFields.LOCATIONS,
                    JobFields.TITLE,
                    JobFields.COMPANY_ID,
                    JobFields.COMPANY_NAME,
                    JobFields.SALARY_MIN,
                    JobFields.SALARY_MAX,
                    JobFields.POSTED_DATE,
                    JobFields.TECHNOLOGIES,
                    JobFields.CITY,
                    JobFields.STATE_REGION,
                    JobFields.SENIORITY_LEVEL,
                    JobFields.COUNTRY,
                    JobFields.SOURCE,
                    JobFields.LAST_SEEN_AT
                )
            )
        } else {
            val techArrayString = techList.joinToString("','", "'", "'")
            JobSimilarQuery(
                sql = """
                    SELECT DISTINCT j.${JobFields.JOB_IDS}, j.${JobFields.APPLY_URLS}, j.${JobFields.PLATFORM_LINKS}, j.${JobFields.LOCATIONS}, j.${JobFields.TITLE}, j.${JobFields.COMPANY_ID}, j.${JobFields.COMPANY_NAME}, j.${JobFields.SALARY_MIN}, j.${JobFields.SALARY_MAX}, j.${JobFields.POSTED_DATE}, j.${JobFields.TECHNOLOGIES}, j.${JobFields.CITY}, j.${JobFields.STATE_REGION}, j.${JobFields.SENIORITY_LEVEL}, j.${JobFields.SOURCE}, j.${JobFields.LAST_SEEN_AT}
                    FROM `$datasetName.$jobsTableName` j, UNNEST(j.${JobFields.TECHNOLOGIES}) t
                    WHERE j.${JobFields.SENIORITY_LEVEL} = @seniority
                      AND @jobId NOT IN UNNEST(j.${JobFields.JOB_IDS})
                      AND t IN ($techArrayString)
                    ORDER BY j.${JobFields.POSTED_DATE} DESC
                    LIMIT 3
                """.trimIndent(),
                requiredFields = listOf(
                    JobFields.JOB_IDS,
                    JobFields.APPLY_URLS,
                    JobFields.PLATFORM_LINKS,
                    JobFields.LOCATIONS,
                    JobFields.TITLE,
                    JobFields.COMPANY_ID,
                    JobFields.COMPANY_NAME,
                    JobFields.SALARY_MIN,
                    JobFields.SALARY_MAX,
                    JobFields.POSTED_DATE,
                    JobFields.TECHNOLOGIES,
                    JobFields.CITY,
                    JobFields.STATE_REGION,
                    JobFields.SENIORITY_LEVEL,
                    JobFields.SOURCE,
                    JobFields.LAST_SEEN_AT
                )
            )
        }
    }
    
    /**
     * Query wrapper for job details.
     */
    data class JobDetailsQuery(
        val sql: String,
        val requiredFields: List<String>
    )
    
    /**
     * Query wrapper for similar jobs.
     */
    data class JobSimilarQuery(
        val sql: String,
        val requiredFields: List<String>
    )
}
