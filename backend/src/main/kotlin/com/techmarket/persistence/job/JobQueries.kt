package com.techmarket.persistence.job

import com.techmarket.persistence.CompanyAliases
import com.techmarket.persistence.CompanyFields
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
                    c.${CompanyFields.COMPANY_ID} as ${CompanyAliases.COMPANY_ID},
                    c.${CompanyFields.NAME} as ${CompanyAliases.NAME},
                    c.${CompanyFields.LOGO_URL} as ${CompanyAliases.LOGO_URL},
                    c.${CompanyFields.DESCRIPTION} as ${CompanyAliases.DESCRIPTION},
                    c.${CompanyFields.WEBSITE} as ${CompanyAliases.WEBSITE},
                    c.${CompanyFields.HIRING_LOCATIONS} as ${CompanyAliases.HIRING_LOCATIONS},
                    c.${CompanyFields.HQ_COUNTRY} as ${CompanyAliases.HQ_COUNTRY},
                    c.${CompanyFields.VERIFICATION_LEVEL} as ${CompanyAliases.VERIFICATION_LEVEL}
                FROM `$datasetName.$jobsTableName` j
                JOIN `$datasetName.$companiesTableName` c ON j.${JobFields.COMPANY_ID} = c.${CompanyFields.COMPANY_ID}
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
                CompanyAliases.COMPANY_ID,
                CompanyAliases.NAME,
                CompanyAliases.LOGO_URL,
                CompanyAliases.DESCRIPTION,
                CompanyAliases.WEBSITE,
                CompanyAliases.HIRING_LOCATIONS,
                CompanyAliases.HQ_COUNTRY,
                CompanyAliases.VERIFICATION_LEVEL
            )
        )
    }
    
    /**
     * Returns SQL for similar jobs query.
     * Includes all fields needed by JobRow.fromJobRow() to prevent silent data degradation.
     */
    fun getSimilarSql(datasetName: String, jobsTableName: String, techList: List<String>): JobSimilarQuery {
        return if (techList.isEmpty()) {
            JobSimilarQuery(
                sql = """
                    SELECT ${JobFields.JOB_ID}, ${JobFields.JOB_IDS}, ${JobFields.APPLY_URLS}, ${JobFields.PLATFORM_LINKS}, ${JobFields.LOCATIONS}, ${JobFields.TITLE}, ${JobFields.COMPANY_ID}, ${JobFields.COMPANY_NAME}, ${JobFields.SALARY_MIN}, ${JobFields.SALARY_MAX}, ${JobFields.POSTED_DATE}, ${JobFields.TECHNOLOGIES}, ${JobFields.BENEFITS}, ${JobFields.CITY}, ${JobFields.STATE_REGION}, ${JobFields.SENIORITY_LEVEL}, ${JobFields.COUNTRY}, ${JobFields.SOURCE}, ${JobFields.LAST_SEEN_AT}, ${JobFields.DESCRIPTION}, ${JobFields.EMPLOYMENT_TYPE}, ${JobFields.WORK_MODEL}, ${JobFields.JOB_FUNCTION}
                    FROM `$datasetName.$jobsTableName`
                    WHERE ${JobFields.SENIORITY_LEVEL} = @seniority
                      AND @jobId NOT IN UNNEST(${JobFields.JOB_IDS})
                    ORDER BY ${JobFields.POSTED_DATE} DESC
                    LIMIT 3
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
                    JobFields.COUNTRY,
                    JobFields.SOURCE,
                    JobFields.LAST_SEEN_AT,
                    JobFields.DESCRIPTION,
                    JobFields.EMPLOYMENT_TYPE,
                    JobFields.WORK_MODEL,
                    JobFields.JOB_FUNCTION
                )
            )
        } else {
            val techArrayString = techList.joinToString("','", "'", "'")
            JobSimilarQuery(
                sql = """
                    SELECT DISTINCT j.${JobFields.JOB_ID}, j.${JobFields.JOB_IDS}, j.${JobFields.APPLY_URLS}, j.${JobFields.PLATFORM_LINKS}, j.${JobFields.LOCATIONS}, j.${JobFields.TITLE}, j.${JobFields.COMPANY_ID}, j.${JobFields.COMPANY_NAME}, j.${JobFields.SALARY_MIN}, j.${JobFields.SALARY_MAX}, j.${JobFields.POSTED_DATE}, j.${JobFields.TECHNOLOGIES}, j.${JobFields.BENEFITS}, j.${JobFields.CITY}, j.${JobFields.STATE_REGION}, j.${JobFields.SENIORITY_LEVEL}, j.${JobFields.COUNTRY}, j.${JobFields.SOURCE}, j.${JobFields.LAST_SEEN_AT}, j.${JobFields.DESCRIPTION}, j.${JobFields.EMPLOYMENT_TYPE}, j.${JobFields.WORK_MODEL}, j.${JobFields.JOB_FUNCTION}
                    FROM `$datasetName.$jobsTableName` j, UNNEST(j.${JobFields.TECHNOLOGIES}) t
                    WHERE j.${JobFields.SENIORITY_LEVEL} = @seniority
                      AND @jobId NOT IN UNNEST(j.${JobFields.JOB_IDS})
                      AND t IN ($techArrayString)
                    ORDER BY j.${JobFields.POSTED_DATE} DESC
                    LIMIT 3
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
                    JobFields.COUNTRY,
                    JobFields.SOURCE,
                    JobFields.LAST_SEEN_AT,
                    JobFields.DESCRIPTION,
                    JobFields.EMPLOYMENT_TYPE,
                    JobFields.WORK_MODEL,
                    JobFields.JOB_FUNCTION
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
