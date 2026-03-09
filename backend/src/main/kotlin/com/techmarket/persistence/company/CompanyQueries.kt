package com.techmarket.persistence.company
 
import com.techmarket.persistence.CompanyFields.ALTERNATE_NAMES
import com.techmarket.persistence.CompanyFields.COMPANY_ID
import com.techmarket.persistence.CompanyFields.DESCRIPTION
import com.techmarket.persistence.CompanyFields.EMPLOYEES_COUNT
import com.techmarket.persistence.CompanyFields.HIRING_LOCATIONS
import com.techmarket.persistence.CompanyFields.HQ_COUNTRY
import com.techmarket.persistence.CompanyFields.INDUSTRIES
import com.techmarket.persistence.CompanyFields.IS_AGENCY
import com.techmarket.persistence.CompanyFields.IS_SOCIAL_ENTERPRISE
import com.techmarket.persistence.CompanyFields.LAST_UPDATED_AT
import com.techmarket.persistence.CompanyFields.LOGO_URL
import com.techmarket.persistence.CompanyFields.NAME
import com.techmarket.persistence.CompanyFields.OFFICE_LOCATIONS
import com.techmarket.persistence.CompanyFields.OPERATING_COUNTRIES
import com.techmarket.persistence.CompanyFields.REMOTE_POLICY
import com.techmarket.persistence.CompanyFields.TECHNOLOGIES
import com.techmarket.persistence.CompanyFields.VERIFICATION_LEVEL
import com.techmarket.persistence.CompanyFields.VISA_SPONSORSHIP
import com.techmarket.persistence.CompanyFields.WEBSITE
import com.techmarket.persistence.JobFields.APPLY_URLS
import com.techmarket.persistence.JobFields.BENEFITS
import com.techmarket.persistence.JobFields.CITY
import com.techmarket.persistence.JobFields.COUNTRY
import com.techmarket.persistence.JobFields.JOB_ID
import com.techmarket.persistence.JobFields.JOB_IDS
import com.techmarket.persistence.JobFields.LAST_SEEN_AT
import com.techmarket.persistence.JobFields.LOCATIONS
import com.techmarket.persistence.JobFields.PLATFORM_LINKS
import com.techmarket.persistence.JobFields.POSTED_DATE
import com.techmarket.persistence.JobFields.SALARY_MAX
import com.techmarket.persistence.JobFields.SALARY_MIN
import com.techmarket.persistence.JobFields.SENIORITY_LEVEL
import com.techmarket.persistence.JobFields.SOURCE
import com.techmarket.persistence.JobFields.STATE_REGION
import com.techmarket.persistence.JobFields.TITLE
import com.techmarket.persistence.JobFields.WORK_MODEL
import com.techmarket.persistence.QueryParams
import com.techmarket.persistence.JobFields.COMPANY_ID as JOB_COMPANY_ID
 
object CompanyQueries {
    
    /**
     * Returns SQL and required fields for company details query.
     * The requiredFields list enables automatic contract testing.
     */
    fun getDetailsSql(datasetName: String, companiesTableName: String): CompanyDetailsQuery {
        return CompanyDetailsQuery(
            sql = """
                SELECT
                    $COMPANY_ID,
                    $NAME,
                    $ALTERNATE_NAMES,
                    $LOGO_URL,
                    $WEBSITE,
                    $EMPLOYEES_COUNT,
                    $INDUSTRIES,
                    $DESCRIPTION,
                    $TECHNOLOGIES,
                    $HIRING_LOCATIONS,
                    $IS_AGENCY,
                    $IS_SOCIAL_ENTERPRISE,
                    $HQ_COUNTRY,
                    $OPERATING_COUNTRIES,
                    $OFFICE_LOCATIONS,
                    $REMOTE_POLICY,
                    $VISA_SPONSORSHIP,
                    $VERIFICATION_LEVEL,
                    $LAST_UPDATED_AT
                FROM `$datasetName.$companiesTableName`
                WHERE $COMPANY_ID = @${QueryParams.COMPANY_ID}
                LIMIT 1
            """.trimIndent(),
            requiredFields = listOf(
                COMPANY_ID,
                NAME,
                ALTERNATE_NAMES,
                LOGO_URL,
                WEBSITE,
                EMPLOYEES_COUNT,
                INDUSTRIES,
                DESCRIPTION,
                TECHNOLOGIES,
                HIRING_LOCATIONS,
                IS_AGENCY,
                IS_SOCIAL_ENTERPRISE,
                HQ_COUNTRY,
                OPERATING_COUNTRIES,
                OFFICE_LOCATIONS,
                REMOTE_POLICY,
                VISA_SPONSORSHIP,
                VERIFICATION_LEVEL,
                LAST_UPDATED_AT
            )
        )
    }
 
    /**
     * Query wrapper that bundles SQL with its required fields.
     * Enables automatic contract testing to prevent "missing field" bugs.
     */
    data class CompanyDetailsQuery(
        val sql: String,
        val requiredFields: List<String>
    )
    
    data class CompanyJobsQuery(
        val sql: String,
        val requiredFields: List<String>
    )
    
    data class CompanyAggQuery(
        val sql: String,
        val requiredFields: List<String>
    )
    
    fun getJobsSql(datasetName: String, jobsTableName: String): CompanyJobsQuery {
        return CompanyJobsQuery(
            sql = """
                SELECT 
                    $JOB_ID, 
                    $JOB_IDS, 
                    $APPLY_URLS, 
                    $PLATFORM_LINKS, 
                    $LOCATIONS, 
                    $TITLE, 
                    $SALARY_MIN, 
                    $SALARY_MAX, 
                    $POSTED_DATE, 
                    $TECHNOLOGIES, 
                    $BENEFITS, 
                    $CITY, 
                    $STATE_REGION, 
                    $SENIORITY_LEVEL, 
                    $SOURCE, 
                    $LAST_SEEN_AT,
                    $COUNTRY,
                    $WORK_MODEL
                FROM `$datasetName.$jobsTableName`
                WHERE $JOB_COMPANY_ID = @${QueryParams.COMPANY_ID}
                AND (@${QueryParams.COUNTRY} IS NULL OR $COUNTRY = @${QueryParams.COUNTRY})
                ORDER BY $POSTED_DATE DESC
            """.trimIndent(),
            requiredFields = listOf(
                JOB_ID,
                JOB_IDS,
                APPLY_URLS,
                PLATFORM_LINKS,
                LOCATIONS,
                TITLE,
                SALARY_MIN,
                SALARY_MAX,
                POSTED_DATE,
                TECHNOLOGIES,
                BENEFITS,
                CITY,
                STATE_REGION,
                SENIORITY_LEVEL,
                SOURCE,
                LAST_SEEN_AT,
                COUNTRY,
                WORK_MODEL
            )
        )
    }
    
    fun getAggSql(datasetName: String, jobsTableName: String): CompanyAggQuery {
        return CompanyAggQuery(
            sql = """
                SELECT MAX($WORK_MODEL) as topModel
                FROM (
                    SELECT $WORK_MODEL, COUNT(*) as c
                    FROM `$datasetName.$jobsTableName`
                    WHERE $JOB_COMPANY_ID = @${QueryParams.COMPANY_ID}
                    AND (@${QueryParams.COUNTRY} IS NULL OR $COUNTRY = @${QueryParams.COUNTRY})
                    GROUP BY $WORK_MODEL
                    ORDER BY c DESC LIMIT 1
                ) wm
            """.trimIndent(),
            requiredFields = listOf(WORK_MODEL)
        )
    }
}
