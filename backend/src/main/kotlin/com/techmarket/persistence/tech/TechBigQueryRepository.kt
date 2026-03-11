package com.techmarket.persistence.tech

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.techmarket.models.CompanyLeaderboardRow
import com.techmarket.models.JobRow
import com.techmarket.models.SeniorityRow
import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.persistence.BigQueryTables
import com.techmarket.persistence.JobFields
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository

@Repository
class TechBigQueryRepository(
        private val bigQuery: BigQuery,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : TechRepository {

        private val jobsTableName = BigQueryTables.JOBS
        private val companiesTableName = BigQueryTables.COMPANIES

        override fun getTechDetails(techName: String, country: String?): TechDetailsPageDto {
                // val formattedTechName = TechFormatter.format(techName)
                // We'll let the mapper handle formatting to keep it consistent

                val senQuery = TechQueries.getSenioritySql(datasetName, jobsTableName)
                val compQuery = TechQueries.getCompaniesSql(datasetName, jobsTableName, companiesTableName)
                val rolesQuery = TechQueries.getJobsSql(datasetName, jobsTableName)

                val senConfig = QueryJobConfiguration.newBuilder(senQuery.sql)
                        .addNamedParameter("techName", QueryParameterValue.string(techName))
                        .addNamedParameter("country", QueryParameterValue.string(country?.lowercase()))

                val compConfig = QueryJobConfiguration.newBuilder(compQuery.sql)
                        .addNamedParameter("techName", QueryParameterValue.string(techName))
                        .addNamedParameter("country", QueryParameterValue.string(country?.lowercase()))

                val rolesConfig = QueryJobConfiguration.newBuilder(rolesQuery.sql)
                        .addNamedParameter("techName", QueryParameterValue.string(techName))
                        .addNamedParameter("country", QueryParameterValue.string(country?.lowercase()))

                val senResult = bigQuery.query(senConfig.build())
                val compResult = bigQuery.query(compConfig.build())
                val rolesResult = bigQuery.query(rolesConfig.build())

                // Hydrate typed rows - all null-safety handled in QueryRows.kt
                val seniorityRows = senResult.values.map { SeniorityRow.fromAggregationRow(it) }
                val companyRows = compResult.values.map { CompanyLeaderboardRow.fromAggregationRow(it) }
                val jobRows = rolesResult.values.map { JobRow.fromJobRow(it) }

                return TechMapper.mapTechDetails(techName, seniorityRows, companyRows, jobRows)
        }
}
