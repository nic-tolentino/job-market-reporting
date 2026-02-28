package com.techmarket.persistence.tech

import com.google.cloud.bigquery.BigQuery
import com.google.cloud.bigquery.QueryJobConfiguration
import com.google.cloud.bigquery.QueryParameterValue
import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.persistence.BigQueryTables
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Repository

@Repository
@Profile("!local")
class TechBigQueryRepository(
        private val bigQuery: BigQuery,
        @Value("\${spring.cloud.gcp.bigquery.dataset-name:techmarket}")
        private val datasetName: String
) : TechRepository {

        private val jobsTableName = BigQueryTables.JOBS
        private val companiesTableName = BigQueryTables.COMPANIES

        override fun getTechDetails(techName: String): TechDetailsPageDto {
                // val formattedTechName = TechFormatter.format(techName)
                // We'll let the mapper handle formatting to keep it consistent

                val senResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(
                                                TechQueries.getSenioritySql(
                                                        datasetName,
                                                        jobsTableName
                                                )
                                        )
                                        .addNamedParameter(
                                                "techName",
                                                QueryParameterValue.string(techName)
                                        )
                                        .build()
                        )
                val compResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(
                                                TechQueries.getCompaniesSql(
                                                        datasetName,
                                                        jobsTableName,
                                                        companiesTableName
                                                )
                                        )
                                        .addNamedParameter(
                                                "techName",
                                                QueryParameterValue.string(techName)
                                        )
                                        .build()
                        )
                val rolesResult =
                        bigQuery.query(
                                QueryJobConfiguration.newBuilder(
                                                TechQueries.getJobsSql(datasetName, jobsTableName)
                                        )
                                        .addNamedParameter(
                                                "techName",
                                                QueryParameterValue.string(techName)
                                        )
                                        .build()
                        )

                return TechMapper.mapTechDetails(techName, senResult, compResult, rolesResult)
        }
}
