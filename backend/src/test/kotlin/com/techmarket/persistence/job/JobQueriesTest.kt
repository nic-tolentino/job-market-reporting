package com.techmarket.persistence.job

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class JobQueriesTest {

    @Test
    fun `getSimilarSql should generate correct SQL without technologies`() {
        val dataset = "test_dataset"
        val table = "test_jobs"
        val techList = emptyList<String>()

        val query = JobQueries.getSimilarSql(dataset, table, techList)
        val sql = query.sql

        assertTrue(sql.contains("FROM `test_dataset.test_jobs`"))
        assertTrue(sql.contains("WHERE seniorityLevel = @seniority"))
        assertTrue(sql.contains("AND @jobId NOT IN UNNEST(jobIds)"))
        assertTrue(sql.contains("ORDER BY postedDate DESC"))
        assertTrue(sql.contains("LIMIT 3"))
        // Should not contain tech queries
        assertTrue(!sql.contains("AND t IN"))
    }

    @Test
    fun `getSimilarSql should generate correct SQL with technologies`() {
        val dataset = "test_dataset"
        val table = "test_jobs"
        val techList = listOf("react", "kotlin")

        val query = JobQueries.getSimilarSql(dataset, table, techList)
        val sql = query.sql

        assertTrue(sql.contains("FROM `test_dataset.test_jobs` j, UNNEST(j.technologies) t"))
        assertTrue(sql.contains("WHERE j.seniorityLevel = @seniority"))
        assertTrue(sql.contains("AND @jobId NOT IN UNNEST(j.jobIds)"))
        assertTrue(sql.contains("AND t IN UNNEST(@techs)"))
        assertTrue(sql.contains("ORDER BY j.postedDate DESC"))
        assertTrue(sql.contains("LIMIT 3"))
    }
}
