package com.techmarket.contract

import com.techmarket.contract.FieldExtractor
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.SalaryFields
import com.techmarket.persistence.company.CompanyQueries
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Automated contract tests to ensure CompanyQueries and Company hydration logic are in sync.
 */
class CompanyQueryMapperContractTest {

    private val relativePath = "src/main/kotlin/com/techmarket/models/QueryRows.kt"

    @Test
    fun `getDetailsSql includes all fields CompanyRow fromCompanyRow reads`() {
        verifyContract(
            methodName = "fromCompanyRow",
            requiredFields = CompanyQueries.getDetailsSql("dataset", "companies").requiredFields,
            "CompanyFields"
        )
    }

    @Test
    fun `getJobsSql includes all fields JobRow fromJobRow reads`() {
        verifyContract(
            methodName = "fromJobRow",
            requiredFields = CompanyQueries.getJobsSql("dataset", "jobs").requiredFields,
            "JobFields", "SalaryFields"
        )
    }
    
    @Test
    fun `getAggSql includes all required fields`() {
        val query = CompanyQueries.getAggSql("dataset", "jobs")
        query.requiredFields.forEach { field ->
            assertTrue(
                query.sql.contains(field, ignoreCase = true),
                "Required field '$field' is missing from getAggSql"
            )
        }
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
        
        // Resolve constant values from their respective objects
        val expectedFieldValues = fieldNames.map { name ->
            val value = lookupFieldValue(name)
            value ?: name // if not in any object, maybe it's a literal access
        }

        val missingFields = expectedFieldValues.filter { !fieldsQueryProvides.contains(it) }

        assertTrue(
            missingFields.isEmpty(),
            "CompanyRow.$methodName reads fields not in requiredFields: $missingFields"
        )

        // Stale fields check
        val staleFields = fieldsQueryProvides - expectedFieldValues.toSet()
        if (staleFields.isNotEmpty()) {
            println("⚠️  WARNING: CompanyQueries.${methodName} query selects fields that row mapper doesn't read: $staleFields")
        }
    }

    private fun lookupFieldValue(name: String): String? {
        if (!name.contains(".")) return null

        val parts = name.split(".")
        val objName = parts[0]
        val fieldName = parts[1]

        val clazz = when (objName) {
            "JobFields" -> JobFields::class.java
            "CompanyFields" -> CompanyFields::class.java
            "SalaryFields" -> SalaryFields::class.java
            else -> null
        }
        
        return try {
            clazz?.getField(fieldName)?.get(null) as? String
        } catch (e: Exception) {
            null
        }
    }
}
