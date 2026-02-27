package com.techmarket.persistence.company

import com.techmarket.api.model.CompanyDetailsDto
import com.techmarket.api.model.CompanyInsightsDto
import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields

object CompanyMapper {
        fun mapCompanyProfile(
                companyId: String,
                detResult: com.google.cloud.bigquery.TableResult,
                jobsResult: com.google.cloud.bigquery.TableResult,
                aggResult: com.google.cloud.bigquery.TableResult
        ): CompanyProfilePageDto {
                val detRow = detResult.values.firstOrNull()
                val name = detRow?.get(CompanyFields.NAME)?.stringValue ?: "Unknown Company"
                val logo =
                        if (detRow?.get(CompanyFields.LOGO_URL)?.isNull == false)
                                detRow.get(CompanyFields.LOGO_URL).stringValue
                        else ""
                val website =
                        if (detRow?.get(CompanyFields.WEBSITE)?.isNull == false)
                                detRow.get(CompanyFields.WEBSITE).stringValue
                        else ""
                val emps =
                        if (detRow?.get(CompanyFields.EMPLOYEES_COUNT)?.isNull == false)
                                detRow.get(CompanyFields.EMPLOYEES_COUNT).longValue.toInt()
                        else 0
                val ind =
                        if (detRow?.get(CompanyFields.INDUSTRIES)?.isNull == false)
                                detRow.get(CompanyFields.INDUSTRIES).stringValue
                        else ""
                val desc =
                        if (detRow?.get(CompanyFields.DESCRIPTION)?.isNull == false)
                                detRow.get(CompanyFields.DESCRIPTION).stringValue
                        else ""

                val details = CompanyDetailsDto(companyId, name, logo, website, emps, ind, desc)

                val roles =
                        jobsResult.values.map { r ->
                                val techList =
                                        if (r.get(JobFields.TECHNOLOGIES).isNull)
                                                emptyList<String>()
                                        else
                                                r.get(JobFields.TECHNOLOGIES).repeatedValue.map {
                                                        it.stringValue
                                                }
                                val city =
                                        if (r.get(JobFields.CITY).isNull) "Unknown"
                                        else r.get(JobFields.CITY).stringValue
                                val stateRegion =
                                        if (r.get(JobFields.STATE_REGION).isNull) "Unknown"
                                        else r.get(JobFields.STATE_REGION).stringValue
                                val locationList =
                                        listOf(
                                                if (stateRegion == "Unknown") city
                                                else "$city, $stateRegion"
                                        )
                                val jobIdList =
                                        if (r.get(JobFields.JOB_IDS).isNull) emptyList<String>()
                                        else
                                                r.get(JobFields.JOB_IDS).repeatedValue.map {
                                                        it.stringValue
                                                }
                                val applyUrlList =
                                        if (r.get(JobFields.APPLY_URLS).isNull) emptyList<String?>()
                                        else
                                                r.get(JobFields.APPLY_URLS).repeatedValue.map {
                                                        if (it.isNull) null else it.stringValue
                                                }
                                JobRoleDto(
                                        id = jobIdList.firstOrNull() ?: "",
                                        title = r.get(JobFields.TITLE).stringValue,
                                        companyId = companyId,
                                        companyName = name,
                                        locations = locationList,
                                        jobIds = jobIdList,
                                        applyUrls = applyUrlList,
                                        salaryMin =
                                                if (r.get(JobFields.SALARY_MIN).isNull) null
                                                else r.get(JobFields.SALARY_MIN).longValue.toInt(),
                                        salaryMax =
                                                if (r.get(JobFields.SALARY_MAX).isNull) null
                                                else r.get(JobFields.SALARY_MAX).longValue.toInt(),
                                        postedDate =
                                                if (r.get(JobFields.POSTED_DATE).isNull) ""
                                                else r.get(JobFields.POSTED_DATE).stringValue,
                                        technologies = techList
                                )
                        }

                val allTechs =
                        if (detRow?.get(CompanyFields.TECHNOLOGIES)?.isNull == false)
                                detRow.get(CompanyFields.TECHNOLOGIES).repeatedValue.map {
                                        it.stringValue
                                }
                        else emptyList()
                val hiringLocations =
                        if (detRow?.get(CompanyFields.HIRING_LOCATIONS)?.isNull == false)
                                detRow.get(CompanyFields.HIRING_LOCATIONS).repeatedValue.map {
                                        it.stringValue
                                }
                        else emptyList()

                val aggRow = aggResult.values.firstOrNull()
                val topModel =
                        if (aggRow?.get("topModel")?.isNull == false)
                                aggRow.get("topModel").stringValue
                        else "Hybrid Friendly"

                val allBenefits =
                        jobsResult
                                .values
                                .mapNotNull { r ->
                                        if (r.get(JobFields.BENEFITS).isNull) null
                                        else
                                                r.get(JobFields.BENEFITS).repeatedValue.map {
                                                        it.stringValue
                                                }
                                }
                                .flatten()
                                .groupingBy { it }
                                .eachCount()
                                .entries
                                .sortedByDescending { it.value }
                                .map { it.key }
                                .take(5)

                val insights = CompanyInsightsDto(topModel, hiringLocations, allBenefits)

                return CompanyProfilePageDto(details, allTechs, insights, roles)
        }
}
