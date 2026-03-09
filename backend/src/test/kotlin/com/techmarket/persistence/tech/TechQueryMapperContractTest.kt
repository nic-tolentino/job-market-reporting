package com.techmarket.persistence.tech

import com.techmarket.persistence.JobFields
import com.techmarket.persistence.JobFields.APPLY_URLS
import com.techmarket.persistence.JobFields.CITY
import com.techmarket.persistence.JobFields.COMPANY_ID
import com.techmarket.persistence.JobFields.COMPANY_NAME
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
 * Tests to ensure TechQueries and TechMapper are in sync.
 * Catches bugs where the mapper tries to read fields not provided by the query.
 */
class TechQueryMapperContractTest {

    @Test
    fun `getJobsSql SELECT clause contains all fields accessed by TechMapper`() {
        val sql = TechQueries.getJobsSql("dataset", "jobs")

        // Fields accessed by TechMapper.mapTechDetails when building job role list
        val requiredFields = listOf(
            JOB_IDS,
            APPLY_URLS,
            PLATFORM_LINKS,
            LOCATIONS,
            TITLE,
            COMPANY_ID,
            COMPANY_NAME,
            SALARY_MIN,
            SALARY_MAX,
            POSTED_DATE,
            TECHNOLOGIES,
            CITY,
            STATE_REGION,
            SENIORITY_LEVEL,
            SOURCE,
            LAST_SEEN_AT
        )

        requiredFields.forEach { field ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by TechMapper but not found in TechQueries.getJobsSql SELECT clause. " +
                "Add it to the query to prevent runtime IllegalArgumentException."
            )
        }
    }

    @Test
    fun `getSenioritySql SELECT clause contains all fields accessed by TechMapper`() {
        val sql = TechQueries.getSenioritySql("dataset", "jobs")

        // Fields accessed by TechMapper.mapTechDetails for seniority distribution
        val requiredFields = listOf(
            "name",  // Alias for SENIORITY_LEVEL
            "value"  // Alias for count
        )

        requiredFields.forEach { field ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by TechMapper but not found in TechQueries.getSenioritySql SELECT clause."
            )
        }
    }

    @Test
    fun `getCompaniesSql SELECT clause contains all fields accessed by TechMapper`() {
        val sql = TechQueries.getCompaniesSql("dataset", "jobs", "companies")

        // Fields accessed by TechMapper.mapTechDetails for company leaderboard
        val requiredFields = listOf(
            "id",
            "name",
            "logo",
            "activeRoles"
        )

        requiredFields.forEach { field ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by TechMapper but not found in TechQueries.getCompaniesSql SELECT clause."
            )
        }
    }
}
