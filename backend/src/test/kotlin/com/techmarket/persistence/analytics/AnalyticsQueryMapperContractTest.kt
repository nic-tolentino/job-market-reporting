package com.techmarket.persistence.analytics

import com.techmarket.persistence.AnalyticsFields
import com.techmarket.persistence.AnalyticsFields.ACTIVE_ROLES
import com.techmarket.persistence.AnalyticsFields.CONTEXT
import com.techmarket.persistence.AnalyticsFields.COUNT
import com.techmarket.persistence.AnalyticsFields.HYBRID_COUNT
import com.techmarket.persistence.AnalyticsFields.ID
import com.techmarket.persistence.AnalyticsFields.LOGO
import com.techmarket.persistence.AnalyticsFields.MESSAGE
import com.techmarket.persistence.AnalyticsFields.NAME
import com.techmarket.persistence.AnalyticsFields.REMOTE_COUNT
import com.techmarket.persistence.AnalyticsFields.TIMESTAMP
import com.techmarket.persistence.AnalyticsFields.TOTAL_VACANCIES
import com.techmarket.persistence.AnalyticsFields.TYPE
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Tests to ensure AnalyticsQueries and AnalyticsMapper are in sync.
 * Catches bugs where the mapper tries to read fields not provided by the query.
 */
class AnalyticsQueryMapperContractTest {

    @Test
    fun `getStatsSql SELECT clause contains all fields accessed by AnalyticsMapper`() {
        val sql = AnalyticsQueries.getStatsSql("dataset", "jobs")

        // Fields accessed by AnalyticsMapper.mapLandingPageData
        val requiredFields: List<String> = listOf(
            TOTAL_VACANCIES,
            REMOTE_COUNT,
            HYBRID_COUNT
        )

        requiredFields.forEach { field: String ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by AnalyticsMapper but not found in AnalyticsQueries.getStatsSql SELECT clause."
            )
        }
    }

    @Test
    fun `getTopTechSql SELECT clause contains all fields accessed by AnalyticsMapper`() {
        val sql = AnalyticsQueries.getTopTechSql("dataset", "jobs")

        // Fields accessed by AnalyticsMapper.mapLandingPageData for tech trends
        val requiredFields: List<String> = listOf(
            NAME,
            COUNT
        )

        requiredFields.forEach { field: String ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by AnalyticsMapper but not found in AnalyticsQueries.getTopTechSql SELECT clause."
            )
        }
    }

    @Test
    fun `getTopCompaniesSql SELECT clause contains all fields accessed by AnalyticsMapper`() {
        val sql = AnalyticsQueries.getTopCompaniesSql("dataset", "jobs", "companies")

        // Fields accessed by AnalyticsMapper.mapLandingPageData for company leaderboard
        val requiredFields: List<String> = listOf(
            ID,
            NAME,
            LOGO,
            ACTIVE_ROLES
        )

        requiredFields.forEach { field: String ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by AnalyticsMapper but not found in AnalyticsQueries.getTopCompaniesSql SELECT clause."
            )
        }
    }

    @Test
    fun `getSearchSuggestionsSql SELECT clause contains all fields accessed by AnalyticsMapper`() {
        val sql = AnalyticsQueries.getSearchSuggestionsSql("dataset", "companies", "jobs")

        // Fields accessed by AnalyticsMapper.mapSearchSuggestion
        val requiredFields: List<String> = listOf(
            TYPE,
            ID,
            NAME
        )

        requiredFields.forEach { field: String ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by AnalyticsMapper but not found in AnalyticsQueries.getSearchSuggestionsSql SELECT clause."
            )
        }
    }

    @Test
    fun `getFeedbackSql SELECT clause contains all fields accessed by AnalyticsMapper`() {
        val sql = AnalyticsQueries.getFeedbackSql("dataset", "feedback")

        // Fields accessed by AnalyticsMapper.mapFeedback
        val requiredFields: List<String> = listOf(
            CONTEXT,
            MESSAGE,
            TIMESTAMP
        )

        requiredFields.forEach { field: String ->
            assertTrue(
                sql.contains(field, ignoreCase = true),
                "Field '$field' is accessed by AnalyticsMapper but not found in AnalyticsQueries.getFeedbackSql SELECT clause."
            )
        }
    }
}
