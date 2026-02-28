package com.techmarket.persistence.analytics

import com.techmarket.api.model.CompanyLeaderboardDto
import com.techmarket.api.model.GlobalStatsDto
import com.techmarket.api.model.LandingPageDto
import com.techmarket.api.model.SearchSuggestionDto
import com.techmarket.api.model.TechTrendAggregatedDto

object AnalyticsMapper {

        fun mapLandingPageData(
                statsResult: com.google.cloud.bigquery.TableResult,
                techResult: com.google.cloud.bigquery.TableResult,
                companiesResult: com.google.cloud.bigquery.TableResult
        ): LandingPageDto {
                val statsRow = statsResult.values.firstOrNull()
                val totalVacancies = statsRow?.get("totalVacancies")?.longValue?.toInt() ?: 0
                val remoteCount = statsRow?.get("remoteCount")?.longValue?.toInt() ?: 0
                val hybridCount = statsRow?.get("hybridCount")?.longValue?.toInt() ?: 0

                val remotePct = if (totalVacancies > 0) (remoteCount * 100) / totalVacancies else 0
                val hybridPct = if (totalVacancies > 0) (hybridCount * 100) / totalVacancies else 0

                val topTechList =
                        techResult.values.map { row ->
                                TechTrendAggregatedDto(
                                        name = row.get("name").stringValue,
                                        count = row.get("count").longValue.toInt(),
                                        percentageChange = 0.0
                                )
                        }
                val topTechName = topTechList.firstOrNull()?.name ?: "N/A"

                val topCompaniesList =
                        companiesResult.values.map { row ->
                                CompanyLeaderboardDto(
                                        id = row.get("id").stringValue,
                                        name = row.get("name").stringValue,
                                        logo =
                                                if (row.get("logo").isNull) ""
                                                else row.get("logo").stringValue,
                                        activeRoles = row.get("activeRoles").longValue.toInt()
                                )
                        }

                return LandingPageDto(
                        globalStats =
                                GlobalStatsDto(totalVacancies, remotePct, hybridPct, topTechName),
                        topTech = topTechList,
                        topCompanies = topCompaniesList
                )
        }

        fun mapSearchSuggestion(
                row: com.google.cloud.bigquery.FieldValueList
        ): SearchSuggestionDto {
                val type = row.get("type").stringValue
                val id = row.get("id").stringValue
                val name = row.get("name").stringValue
                return SearchSuggestionDto(type, id, name)
        }

        fun mapFeedback(
                row: com.google.cloud.bigquery.FieldValueList
        ): com.techmarket.api.model.FeedbackDto {
                val context =
                        if (row.get("context").isNull) null else row.get("context").stringValue
                val message = row.get("message").stringValue
                val timestamp = row.get("timestamp").stringValue
                return com.techmarket.api.model.FeedbackDto(context, message, timestamp)
        }
}
