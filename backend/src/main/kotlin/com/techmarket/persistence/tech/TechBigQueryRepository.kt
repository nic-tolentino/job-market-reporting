package com.techmarket.persistence.tech

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
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

                val senConfig = QueryJobConfiguration.newBuilder(
                        TechQueries.getSenioritySql(datasetName, jobsTableName)
                ).addNamedParameter("techName", QueryParameterValue.string(techName))

                val compConfig = QueryJobConfiguration.newBuilder(
                        TechQueries.getCompaniesSql(datasetName, jobsTableName, companiesTableName)
                ).addNamedParameter("techName", QueryParameterValue.string(techName))

                val rolesConfig = QueryJobConfiguration.newBuilder(
                        TechQueries.getJobsSql(datasetName, jobsTableName)
                ).addNamedParameter("techName", QueryParameterValue.string(techName))

                if (country != null) {
                        val c = country.lowercase()
                        senConfig.addNamedParameter(JobFields.COUNTRY, com.google.cloud.bigquery.QueryParameterValue.string(c))
                        compConfig.addNamedParameter(JobFields.COUNTRY, com.google.cloud.bigquery.QueryParameterValue.string(c))
                        rolesConfig.addNamedParameter(JobFields.COUNTRY, com.google.cloud.bigquery.QueryParameterValue.string(c))
                }

                val senResult = bigQuery.query(senConfig.build())
                val compResult = bigQuery.query(compConfig.build())
                val rolesResult = bigQuery.query(rolesConfig.build())

                return TechMapper.mapTechDetails(techName, senResult, compResult, rolesResult)
        }
}
