package com.techmarket.contract

import com.techmarket.persistence.tech.TechQueries
import com.techmarket.persistence.JobFields
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Automated contract tests to ensure TechQueries and TechMapper are in sync.
 */
class TechQueryMapperContractTest {

    private val relativePath = "src/main/kotlin/com/techmarket/persistence/tech/TechMapper.kt"

    @Test
    fun `getJobsSql includes all fields mapJobRole reads`() {
        verifyContract(
            methodName = "mapJobRole",
            requiredFields = TechQueries.getJobsSql("dataset", "jobs").requiredFields,
            "JobFields"
        )
    }

    @Test
    fun `getSenioritySql includes all fields mapSeniorityLine reads`() {
        verifyContract(
            methodName = "mapSeniorityLine",
            requiredFields = TechQueries.getSenioritySql("dataset", "jobs").requiredFields
            // uses literals directly
        )
    }

    @Test
    fun `getCompaniesSql includes all fields mapCompanyLine reads`() {
        verifyContract(
            methodName = "mapCompanyLine",
            requiredFields = TechQueries.getCompaniesSql("dataset", "jobs", "companies").requiredFields
            // uses literals directly
        )
    }

    private fun verifyContract(
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
