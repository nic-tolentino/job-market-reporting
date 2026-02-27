package com.techmarket.persistence.tech

import com.techmarket.api.model.CompanyLeaderboardDto
import com.techmarket.api.model.SeniorityDistributionDto
import com.techmarket.api.model.TechDetailsPageDto

object TechMapper {
    fun mapTechDetails(
            formattedTechName: String,
            senResult: com.google.cloud.bigquery.TableResult,
            compResult: com.google.cloud.bigquery.TableResult
    ): TechDetailsPageDto {

        val seniorityDistribution =
                senResult.values.map {
                    SeniorityDistributionDto(
                            it.get("name").stringValue,
                            it.get("value").longValue.toInt()
                    )
                }

        val companies =
                compResult.values.map {
                    CompanyLeaderboardDto(
                            id = it.get("id").stringValue,
                            name = it.get("name").stringValue,
                            logo = if (it.get("logo").isNull) "" else it.get("logo").stringValue,
                            activeRoles = it.get("activeRoles").longValue.toInt()
                    )
                }

        val totalJobs = seniorityDistribution.sumOf { it.value }

        return TechDetailsPageDto(formattedTechName, totalJobs, seniorityDistribution, companies)
    }
}
