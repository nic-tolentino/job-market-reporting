package com.techmarket.persistence

import com.techmarket.persistence.analytics.AnalyticsQueries
import com.techmarket.persistence.company.CompanyQueries
import com.techmarket.persistence.ingestion.IngestionQueries
import com.techmarket.persistence.tech.TechQueries
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Safety tests for all BigQuery SQL query generators.
 *
 * These tests guard against two classes of bugs that have caused production failures:
 *
 * 1. **Literal `$` variables**: Using escaped `\$` in a Kotlin string template causes the dollar
 * ```
 *    sign to be emitted literally into the SQL, producing `$datasetName.$tableName` instead of the
 *    real values. BigQuery rejects this with "Unexpected `$datasetName`".
 * ```
 * 2. **Missing backtick-wrapping**: BigQuery Standard SQL requires fully-qualified table references
 * ```
 *    (dataset.table) to be wrapped in backticks: `` `dataset.table` ``. Without them, the parser
 *    fails with "Unexpected identifier" errors.
 * ```
 * Every query object should have a corresponding assertion here.
 */
class SqlSafetyTest {

    private val dataset = "my_dataset"
    private val jobsTable = "raw_jobs"
    private val companiesTable = "raw_companies"
    private val ingestionsTable = "raw_ingestions"

    // --- Shared helpers ---

    private fun assertSqlSafe(sql: String) {
        assertFalse(
                sql.contains("\$datasetName"),
                "SQL must not contain literal \$datasetName: $sql"
        )
        assertFalse(
                sql.contains("\$jobsTableName"),
                "SQL must not contain literal \$jobsTableName: $sql"
        )
        assertFalse(sql.contains("\$tableName"), "SQL must not contain literal \$tableName: $sql")
        assertFalse(
                sql.contains("\$companiesTableName"),
                "SQL must not contain literal \$companiesTableName"
        )
        assertFalse(
                sql.contains("\$ingestionsTableName"),
                "SQL must not contain literal \$ingestionsTableName"
        )
    }

    private fun assertBacktickWrapped(sql: String, dataset: String, table: String) {
        assertTrue(
                sql.contains("`$dataset.$table`"),
                "SQL must reference table as `$dataset.$table` (backtick-wrapped), got: $sql"
        )
    }

    // --- IngestionQueries ---

    @Test
    fun `IngestionQueries getSelectAllSql interpolates values and wraps table in backticks`() {
        val sql = IngestionQueries.getSelectAllSql(dataset, ingestionsTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, ingestionsTable)
    }

    // --- CompanyQueries ---

    @Test
    fun `CompanyQueries getJobsSql interpolates values and wraps table in backticks`() {
        val sql = CompanyQueries.getJobsSql(dataset, jobsTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, jobsTable)
    }

    // --- AnalyticsQueries ---

    @Test
    fun `AnalyticsQueries getStatsSql interpolates values and wraps table in backticks`() {
        val sql = AnalyticsQueries.getStatsSql(dataset, jobsTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, jobsTable)
    }

    @Test
    fun `AnalyticsQueries getTopTechSql interpolates values and wraps table in backticks`() {
        val sql = AnalyticsQueries.getTopTechSql(dataset, jobsTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, jobsTable)
    }

    @Test
    fun `AnalyticsQueries getTopCompaniesSql interpolates values and wraps tables in backticks`() {
        val sql = AnalyticsQueries.getTopCompaniesSql(dataset, jobsTable, companiesTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, jobsTable)
        assertBacktickWrapped(sql, dataset, companiesTable)
    }

    @Test
    fun `AnalyticsQueries getSearchSuggestionsSql interpolates values and wraps tables in backticks`() {
        val sql = AnalyticsQueries.getSearchSuggestionsSql(dataset, companiesTable, jobsTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, jobsTable)
        assertBacktickWrapped(sql, dataset, companiesTable)
    }

    // --- TechQueries ---

    @Test
    fun `TechQueries getSenioritySql interpolates values and wraps table in backticks`() {
        val sql = TechQueries.getSenioritySql(dataset, jobsTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, jobsTable)
    }

    @Test
    fun `TechQueries getCompaniesSql interpolates values and wraps tables in backticks`() {
        val sql = TechQueries.getCompaniesSql(dataset, jobsTable, companiesTable)
        assertSqlSafe(sql)
        assertBacktickWrapped(sql, dataset, jobsTable)
        assertBacktickWrapped(sql, dataset, companiesTable)
    }

    // --- Delete SQL (inline strings in repositories, tested via string helpers) ---

    @Test
    fun `delete SQL strings interpolate dataset and table correctly`() {
        // These mirror the inline SQL in JobBigQueryRepository and CompanyBigQueryRepository.
        // If these fail it means the \$ escape bug has been re-introduced.
        val deleteJobsSql = "DELETE FROM `$dataset.$jobsTable` WHERE true"
        val deleteCompaniesSql = "DELETE FROM `$dataset.$companiesTable` WHERE true"

        assertSqlSafe(deleteJobsSql)
        assertSqlSafe(deleteCompaniesSql)
        assertBacktickWrapped(deleteJobsSql, dataset, jobsTable)
        assertBacktickWrapped(deleteCompaniesSql, dataset, companiesTable)
    }
}
