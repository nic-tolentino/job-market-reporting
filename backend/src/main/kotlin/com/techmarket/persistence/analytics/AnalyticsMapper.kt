package com.techmarket.persistence.analytics

import com.techmarket.api.model.CompanyLeaderboardDto
import com.techmarket.api.model.FeedbackDto
import com.techmarket.api.model.GlobalStatsDto
import com.techmarket.api.model.LandingPageDto
import com.techmarket.api.model.SearchSuggestionDto
import com.techmarket.api.model.TechTrendAggregatedDto
import com.techmarket.persistence.AnalyticsFields
import com.techmarket.util.TechFormatter

object AnalyticsMapper {

        fun mapLandingPageData(
                statsResult: com.google.cloud.bigquery.TableResult,
                techResult: com.google.cloud.bigquery.TableResult,
                companiesResult: com.google.cloud.bigquery.TableResult
        ): LandingPageDto {
                val statsRow = statsResult.values.firstOrNull()
                val globalStats = mapGlobalStats(statsRow)
                
                val topTechList = techResult.values.map { mapTechTrend(it) }
                val topTechName = topTechList.firstOrNull()?.name ?: "N/A"

                val topCompaniesList = companiesResult.values.map { mapCompanyLeaderboard(it) }

                return LandingPageDto(
                        globalStats = globalStats.copy(topTech = TechFormatter.format(topTechName)),
                        topTech = topTechList,
                        topCompanies = topCompaniesList
                )
        }

        fun mapGlobalStats(row: com.google.cloud.bigquery.FieldValueList?): GlobalStatsDto {
                val totalVacancies = row?.get(AnalyticsFields.TOTAL_VACANCIES)?.longValue?.toInt() ?: 0
                val remoteCount = row?.get(AnalyticsFields.REMOTE_COUNT)?.longValue?.toInt() ?: 0
                val hybridCount = row?.get(AnalyticsFields.HYBRID_COUNT)?.longValue?.toInt() ?: 0

                val remotePct = if (totalVacancies > 0) (remoteCount * 100) / totalVacancies else 0
                val hybridPct = if (totalVacancies > 0) (hybridCount * 100) / totalVacancies else 0

                return GlobalStatsDto(
                        totalVacancies = totalVacancies,
                        remotePercentage = remotePct,
                        hybridPercentage = hybridPct,
                        topTech = "" // Filled by mapLandingPageData
                )
        }

        fun mapTechTrend(row: com.google.cloud.bigquery.FieldValueList): TechTrendAggregatedDto {
                return TechTrendAggregatedDto(
                        name = TechFormatter.format(row.get(AnalyticsFields.NAME).stringValue),
                        count = row.get(AnalyticsFields.COUNT).longValue.toInt(),
                        percentageChange = 0.0
                )
        }

        fun mapCompanyLeaderboard(row: com.google.cloud.bigquery.FieldValueList): CompanyLeaderboardDto {
                return CompanyLeaderboardDto(
                        id = row.get(AnalyticsFields.ID).stringValue,
                        name = row.get(AnalyticsFields.NAME).stringValue,
                        logo = if (row.get(AnalyticsFields.LOGO).isNull) "" else row.get(AnalyticsFields.LOGO).stringValue,
                        activeRoles = row.get(AnalyticsFields.ACTIVE_ROLES).longValue.toInt()
                )
        }

        fun mapSearchSuggestion(
                row: com.google.cloud.bigquery.FieldValueList
        ): SearchSuggestionDto {
                val type = row.get(AnalyticsFields.TYPE).stringValue
                val id = row.get(AnalyticsFields.ID).stringValue
                val name = row.get(AnalyticsFields.NAME).stringValue
                return SearchSuggestionDto(type, id, name)
        }

        fun mapFeedback(
                row: com.google.cloud.bigquery.FieldValueList
        ): FeedbackDto {
                val context =
                        if (row.get(AnalyticsFields.CONTEXT).isNull) null else row.get(AnalyticsFields.CONTEXT).stringValue
                val message = row.get(AnalyticsFields.MESSAGE).stringValue
                val timestamp = row.get(AnalyticsFields.TIMESTAMP).stringValue
                return FeedbackDto(context, message, timestamp)
        }
}
