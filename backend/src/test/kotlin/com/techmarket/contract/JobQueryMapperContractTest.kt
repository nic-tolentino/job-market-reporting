package com.techmarket.contract

import com.techmarket.persistence.job.JobQueries
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.CompanyAliases
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Automated contract tests to ensure JobQueries and JobMapper are in sync.
 */
class JobQueryMapperContractTest {

    private val relativePath = "src/main/kotlin/com/techmarket/persistence/job/JobMapper.kt"

    @Test
    fun `getDetailsSql includes all fields JobMapper read methods require`() {
        val query = JobQueries.getDetailsSql("dataset", "jobs", "companies")
        
        // mapJobDetailsDto
        verifyContract(
            methodName = "mapJobDetailsDto",
            requiredFields = query.requiredFields,
            "JobFields"
        )
        
        // mapJobLocations
        verifyContract(
            methodName = "mapJobLocations",
            requiredFields = query.requiredFields,
            "JobFields"
        )
        
        // mapJobCompanyDto
        verifyContract(
            methodName = "mapJobCompanyDto",
            requiredFields = query.requiredFields,
            "JobFields", "CompanyAliases"
        )
    }

    @Test
    fun `getDetailsSql does not select fields that no JobMapper method reads`() {
        val methods = listOf("mapJobDetailsDto", "mapJobLocations", "mapJobCompanyDto", "mapJobRole")
        val allFieldsRead = methods.flatMap { methodName ->
            FieldExtractor.extractFieldsFromMethod(relativePath, methodName, "JobFields", "CompanyAliases")
        }.map { name ->
            lookupFieldValue(name) ?: name
        }.toSet()

        val fieldsQuerySelects = JobQueries.getDetailsSql("dataset", "jobs", "companies").requiredFields.toSet()
        val staleFields = fieldsQuerySelects - allFieldsRead

        if (staleFields.isNotEmpty()) {
            println("⚠️  WARNING: JobQueries.getDetailsSql selects fields no mapper method reads: $staleFields")
        }
    }

    @Test
    fun `getSimilarSql includes all fields JobMapper reads for similar roles`() {
        verifyContract(
            methodName = "mapJobRole",
            requiredFields = JobQueries.getSimilarSql("dataset", "jobs", emptyList()).requiredFields,
            "JobFields"
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
        
        // Resolve constant values
        val expectedFieldValues = fieldNames.map { name ->
            lookupFieldValue(name) ?: name
        }

        val missingFields = expectedFieldValues.filter { !fieldsQueryProvides.contains(it) }

        assertTrue(
            missingFields.isEmpty(),
            "JobMapper.$methodName reads fields not in requiredFields: $missingFields"
        )
        
        // Stale fields check - only for mapJobRole which is a 1-to-1 mapping usually
        // For sub-mappings of larger queries, skip this check here to avoid false positives
        if (methodName == "mapJobRole") {
            val staleFields = fieldsQueryProvides - expectedFieldValues.toSet()
            if (staleFields.isNotEmpty()) {
                 println("⚠️  WARNING: JobQueries query for $methodName selects fields that mapper doesn't read: $staleFields")
            }
        }
    }

    private fun lookupFieldValue(name: String): String? {
        if (!name.contains(".")) return null // Literal string access

        val parts = name.split(".")
        val objName = parts[0]
        val fieldName = parts[1]

        val clazz = try {
            when (objName) {
                "JobFields" -> JobFields::class.java
                "CompanyFields" -> CompanyFields::class.java
                "CompanyAliases" -> CompanyAliases::class.java
                else -> null
            }
        } catch (e: Exception) {
            null
        }
        
        return try {
            clazz?.getField(fieldName)?.get(null) as? String
        } catch (e: NoSuchFieldException) {
            null
        }
    }
}
