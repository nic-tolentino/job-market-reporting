package com.techmarket.persistence.tech

import com.techmarket.api.model.CompanyLeaderboardDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.api.model.SeniorityDistributionDto
import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.persistence.JobFields

object TechMapper {
        fun mapTechDetails(
                formattedTechName: String,
                senResult: com.google.cloud.bigquery.TableResult,
                compResult: com.google.cloud.bigquery.TableResult,
                rolesResult: com.google.cloud.bigquery.TableResult
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
                                        logo =
                                                if (it.get("logo").isNull) ""
                                                else it.get("logo").stringValue,
                                        activeRoles = it.get("activeRoles").longValue.toInt()
                                )
                        }

                val roles =
                        rolesResult.values.map { row ->
                                val idList =
                                        if (row.get(JobFields.JOB_IDS).isNull) emptyList<String>()
                                        else
                                                row.get(JobFields.JOB_IDS).repeatedValue.map {
                                                        it.stringValue
                                                }
                                val applyList =
                                        if (row.get(JobFields.APPLY_URLS).isNull)
                                                emptyList<String?>()
                                        else
                                                row.get(JobFields.APPLY_URLS).repeatedValue.map {
                                                        if (it.isNull) null else it.stringValue
                                                }
                                val techList =
                                        if (row.get(JobFields.TECHNOLOGIES).isNull)
                                                emptyList<String>()
                                        else
                                                row.get(JobFields.TECHNOLOGIES).repeatedValue.map {
                                                        it.stringValue
                                                }

                                val city =
                                        if (row.get(JobFields.CITY).isNull) "Unknown"
                                        else row.get(JobFields.CITY).stringValue
                                val stateRegion =
                                        if (row.get(JobFields.STATE_REGION).isNull) "Unknown"
                                        else row.get(JobFields.STATE_REGION).stringValue
                                val locList =
                                        listOf(
                                                if (stateRegion == "Unknown") city
                                                else "$city, $stateRegion"
                                        )

                                JobRoleDto(
                                        id = idList.firstOrNull() ?: "",
                                        title = row.get(JobFields.TITLE).stringValue,
                                        companyId = row.get(JobFields.COMPANY_ID).stringValue,
                                        companyName =
                                                if (row.get(JobFields.COMPANY_NAME).isNull) ""
                                                else row.get(JobFields.COMPANY_NAME).stringValue,
                                        locations = locList,
                                        jobIds = idList,
                                        applyUrls = applyList,
                                        salaryMin =
                                                if (row.get(JobFields.SALARY_MIN).isNull) null
                                                else
                                                        row.get(JobFields.SALARY_MIN)
                                                                .longValue
                                                                .toInt(),
                                        salaryMax =
                                                if (row.get(JobFields.SALARY_MAX).isNull) null
                                                else
                                                        row.get(JobFields.SALARY_MAX)
                                                                .longValue
                                                                .toInt(),
                                        postedDate =
                                                if (row.get(JobFields.POSTED_DATE).isNull) ""
                                                else row.get(JobFields.POSTED_DATE).stringValue,
                                        seniorityLevel =
                                                row.get(JobFields.SENIORITY_LEVEL).stringValue,
                                        technologies = techList
                                )
                        }

                val totalJobs = seniorityDistribution.sumOf { it.value }

                return TechDetailsPageDto(
                        formattedTechName,
                        totalJobs,
                        seniorityDistribution,
                        companies,
                        roles
                )
        }
}
