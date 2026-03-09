package com.techmarket.persistence.job

import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.JobFields.APPLY_URLS
import com.techmarket.persistence.JobFields.BENEFITS
import com.techmarket.persistence.JobFields.CITY
import com.techmarket.persistence.JobFields.COMPANY_ID
import com.techmarket.persistence.JobFields.COMPANY_NAME
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
import com.techmarket.persistence.JobFields.TECHNOLOGIES
import com.techmarket.persistence.JobFields.TITLE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Tests to ensure JobQueries and JobMapper are in sync.
 * Catches bugs where the mapper tries to read fields not provided by the query.
 */
class JobQueryMapperContractTest {

    @Test
    fun `getDetailsSql SELECT clause contains all fields accessed by JobMapper`() {
        // Get the actual SQL query
        val query = JobQueries.getDetailsSql("dataset", "jobs", "companies")
        val sql = query.sql

        // Note: getDetailsSql uses "j.*" which selects all job fields automatically
        // We only need to verify the company fields (which use aliases) are present
        val requiredCompanyFields = mapOf(
            "comp_name" to CompanyFields.NAME,
            "comp_logo" to CompanyFields.LOGO_URL,
            "comp_desc" to CompanyFields.DESCRIPTION,
            "comp_web" to CompanyFields.WEBSITE,
            "comp_hiringLocations" to CompanyFields.HIRING_LOCATIONS,
            "comp_hqCountry" to CompanyFields.HQ_COUNTRY,
            "comp_verificationLevel" to CompanyFields.VERIFICATION_LEVEL
        )

        // Verify each required company field alias appears in the SELECT clause
        requiredCompanyFields.forEach { (alias, field) ->
            assertTrue(
                sql.contains(alias, ignoreCase = true),
                "Company field alias '$alias' (for $field) is accessed by JobMapper but not found in getDetailsSql SELECT clause."
            )
        }
    }

    @Test
    fun `getJobsSql SELECT clause contains all fields accessed by CompanyMapper`() {
        val query = com.techmarket.persistence.company.CompanyQueries.getJobsSql("dataset", "jobs")
        val sql = query.sql

        // Fields accessed by CompanyMapper when building company profile job list
        val requiredFields = listOf(
            JOB_ID,
            TITLE,
            SALARY_MIN,
            SALARY_MAX,
            POSTED_DATE,
            SENIORITY_LEVEL,
            SOURCE,
            TECHNOLOGIES,
            BENEFITS,
            CITY,
            STATE_REGION
        )

        requiredFields.forEach { field ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by CompanyMapper but not found in getJobsSql SELECT clause."
            )
        }
    }
}
