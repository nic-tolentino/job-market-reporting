package com.techmarket.contract

import com.techmarket.persistence.analytics.AnalyticsQueries
import com.techmarket.persistence.AnalyticsFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Automated contract tests to ensure AnalyticsQueries and AnalyticsMapper are in sync.
 */
class AnalyticsQueryMapperContractTest {

    private val relativePath = "src/main/kotlin/com/techmarket/persistence/analytics/AnalyticsMapper.kt"

    @Test
    fun `getStatsSql includes all fields mapGlobalStats reads`() {
        verifyContract(
            methodName = "mapGlobalStats",
            requiredFields = AnalyticsQueries.getStatsSql("dataset", "jobs").requiredFields
        )
    }

    @Test
    fun `getTopTechSql includes all fields mapTechTrend reads`() {
        verifyContract(
            methodName = "mapTechTrend",
            requiredFields = AnalyticsQueries.getTopTechSql("dataset", "jobs").requiredFields
        )
    }

    @Test
    fun `getTopCompaniesSql includes all fields mapCompanyLeaderboard reads`() {
        verifyContract(
            methodName = "mapCompanyLeaderboard",
            requiredFields = AnalyticsQueries.getTopCompaniesSql("dataset", "jobs", "companies").requiredFields
        )
    }

    @Test
    fun `getSearchSuggestionsSql includes all fields mapSearchSuggestion reads`() {
        verifyContract(
            methodName = "mapSearchSuggestion",
            requiredFields = AnalyticsQueries.getSearchSuggestionsSql("dataset", "companies", "jobs").requiredFields
        )
    }

    @Test
    fun `getFeedbackSql includes all fields mapFeedback reads`() {
        verifyContract(
            methodName = "mapFeedback",
            requiredFields = AnalyticsQueries.getFeedbackSql("dataset", "feedback").requiredFields
        )
    }

    private fun verifyContract(methodName: String, requiredFields: List<String>) {
        val fieldNames = FieldExtractor.extractFieldsFromMethod(
            relativePath,
            methodName,
            "AnalyticsFields"
        )

        val fieldsQueryProvides = requiredFields.toSet()
        val fieldValues = fieldNames.map { name ->
            lookupFieldValue(name) ?: name
        }

        // Missing fields check
        val missingFields = fieldValues.filter { !fieldsQueryProvides.contains(it) }
        assertTrue(
            missingFields.isEmpty(),
            "AnalyticsMapper.$methodName reads fields not in requiredFields: $missingFields"
        )

        // Stale fields check
        val staleFields = fieldsQueryProvides - fieldValues.toSet()
        if (staleFields.isNotEmpty()) {
            println("⚠️  WARNING: ${methodName} query selects fields that mapper doesn't read: $staleFields")
        }
    }

    private fun lookupFieldValue(name: String): String? {
        if (!name.contains(".")) return null

        val parts = name.split(".")
        val objName = parts[0]
        val fieldName = parts[1]

        val clazz = when (objName) {
            "AnalyticsFields" -> AnalyticsFields::class.java
            else -> null
        }
        
        return try {
            clazz?.getField(fieldName)?.get(null) as? String
        } catch (e: Exception) {
            null
        }
    }
}
