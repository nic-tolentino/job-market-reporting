package com.techmarket.persistence.company

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Generic contract test that verifies all queries include their required fields.
 * This replaces the reflection-based source code parsing with a simpler approach.
 * 
 * If a query is missing a required field, this test will fail at compile/test time,
 * not at runtime when a user hits a 500 error.
 */
class CompanyQueriesContractTest {

    @Test
    fun `getDetailsSql includes all required fields`() {
        val query = CompanyQueries.getDetailsSql("dataset", "companies")
        query.requiredFields.forEach { field ->
            assertTrue(
                query.sql.contains(field, ignoreCase = true),
                "Required field '$field' is missing from getDetailsSql"
            )
        }
    }

    @Test
    fun `getJobsSql includes all required fields`() {
        val query = CompanyQueries.getJobsSql("dataset", "jobs")
        query.requiredFields.forEach { field ->
            assertTrue(
                query.sql.contains(field, ignoreCase = true),
                "Required field '$field' is missing from getJobsSql"
            )
        }
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
}
