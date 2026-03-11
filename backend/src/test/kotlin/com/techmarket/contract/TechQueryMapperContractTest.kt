package com.techmarket.contract

import com.techmarket.persistence.tech.TechQueries
import com.techmarket.persistence.JobFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Automated contract tests to ensure TechQueries and TechMapper are in sync.
 * Note: TechMapper delegates to JobRowMapper for mapJobRole, so we test JobRowMapper instead.
 */
class TechQueryMapperContractTest {

    private val techMapperPath = "src/main/kotlin/com/techmarket/persistence/tech/TechMapper.kt"
    private val jobRowMapperPath = "src/main/kotlin/com/techmarket/persistence/JobRowMapper.kt"

    @Test
    fun `getJobsSql includes all fields mapToJobRole reads`() {
        verifyContract(
            relativePath = jobRowMapperPath,
            methodName = "mapToJobRole",
            requiredFields = TechQueries.getJobsSql("dataset", "jobs").requiredFields,
            "JobFields"
        )
    }

    @Test
    fun `getSenioritySql includes all fields mapSeniorityLine reads`() {
        verifyContract(
            relativePath = techMapperPath,
            methodName = "mapSeniorityLine",
            requiredFields = TechQueries.getSenioritySql("dataset", "jobs").requiredFields
            // uses literals directly
        )
    }

    @Test
    fun `getCompaniesSql includes all fields mapCompanyLine reads`() {
        verifyContract(
            relativePath = techMapperPath,
            methodName = "mapCompanyLine",
            requiredFields = TechQueries.getCompaniesSql("dataset", "jobs", "companies").requiredFields
            // uses literals directly
        )
    }

    private fun verifyContract(
        relativePath: String,
        methodName: String,
        requiredFields: List<String>,
        vararg fieldObjectNames: String
    ) {
        val fieldNames = FieldExtractor.extractFieldsFromMethod(
            relativePath,
            methodName,
            *fieldObjectNames
        )

        val fieldsQueryProvides = requiredFields.toSet()

        // Resolve constant values or use literals
        val expectedFields = fieldNames.map { name ->
            lookupFieldValue(name) ?: name
        }

        val missingFields = expectedFields.filter { !fieldsQueryProvides.contains(it) }
        assertTrue(
            missingFields.isEmpty(),
            "TechMapper.$methodName reads fields not in requiredFields: $missingFields"
        )

        // Stale fields check
        val staleFields = fieldsQueryProvides - expectedFields.toSet()
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
            "JobFields" -> JobFields::class.java
            else -> null
        }

        return try {
            clazz?.getField(fieldName)?.get(null) as? String
        } catch (e: Exception) {
            null
        }
    }
}
