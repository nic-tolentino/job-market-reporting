package com.techmarket.persistence.tech

import com.techmarket.api.model.CompanyLeaderboardDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.api.model.SeniorityDistributionDto
import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.SalaryMapper
import com.techmarket.util.TechFormatter
import java.time.Instant

object TechMapper {
        fun mapTechDetails(
                techName: String,
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
                                val linkList =
                                        if (row.get(JobFields.PLATFORM_LINKS).isNull) emptyList<String?>()
                                        else
                                                row.get(JobFields.PLATFORM_LINKS).repeatedValue.map {
                                                        if (it.isNull) null else it.stringValue
                                                }
                                val techList =
                                        if (row.get(JobFields.TECHNOLOGIES).isNull)
                                                emptyList<String>()
                                        else
                                                row.get(JobFields.TECHNOLOGIES).repeatedValue.map {
                                                        TechFormatter.format(it.stringValue)
                                                }

                                val city =
                                        if (row.get(JobFields.CITY).isNull) "Unknown"
                                        else row.get(JobFields.CITY).stringValue
                                val stateRegion =
                                        if (row.get(JobFields.STATE_REGION).isNull) "Unknown"
                                        else row.get(JobFields.STATE_REGION).stringValue
                                val locList =
                                        listOf(
                                                if (stateRegion == "Unknown" || stateRegion == city)
                                                        city
                                                else "$city, $stateRegion"
                                        )

                                val rowSource =
                                        if (row.get(JobFields.SOURCE).isNull) "Unknown"
                                        else row.get(JobFields.SOURCE).stringValue
                                val rowLastUpdatedAt =
                                        if (row.get(JobFields.LAST_SEEN_AT).isNull) Instant.EPOCH
                                        else parseTimestampSafe(row.get(JobFields.LAST_SEEN_AT))
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
                                        platformLinks = linkList,
                                        salaryMin = SalaryMapper.fromFieldValue(row, JobFields.SALARY_MIN),
                                        salaryMax = SalaryMapper.fromFieldValue(row, JobFields.SALARY_MAX),
                                        postedDate =
                                                if (row.get(JobFields.POSTED_DATE).isNull) ""
                                                else row.get(JobFields.POSTED_DATE).stringValue,
                                        seniorityLevel =
                                                row.get(JobFields.SENIORITY_LEVEL).stringValue,
                                        technologies = techList,
                                        source = rowSource,
                                        lastUpdatedAt = rowLastUpdatedAt
                                )
                        }

                val totalJobs = seniorityDistribution.sumOf { it.value }

                return TechDetailsPageDto(
                        techName = TechFormatter.format(techName),
                        totalJobs,
                        seniorityDistribution,
                        companies,
                        roles
                )
        }

        private fun parseTimestampSafe(field: com.google.cloud.bigquery.FieldValue): Instant {
                if (field.isNull) return Instant.EPOCH
                return try {
                        val stringVal = field.stringValue
                        val doubleVal = stringVal.toDoubleOrNull()
                        if (doubleVal != null) {
                                Instant.ofEpochSecond(doubleVal.toLong())
                        } else {
                                Instant.parse(stringVal)
                        }
                } catch (e: Exception) {
                        Instant.EPOCH
                }
        }
}
