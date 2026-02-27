package com.techmarket.persistence.job

import com.techmarket.api.model.JobRoleDto
import com.techmarket.persistence.JobFields
import kotlin.math.min

object JobMapper {
    fun mapJobDetails(
            r: com.google.cloud.bigquery.FieldValueList,
            techList: List<String>,
            similarResult: com.google.cloud.bigquery.TableResult
    ): com.techmarket.api.model.JobPageDto {

        val benefitList =
                if (r.get(JobFields.BENEFITS).isNull) null
                else r.get(JobFields.BENEFITS).repeatedValue.map { it.stringValue }

        val details =
                com.techmarket.api.model.JobDetailsDto(
                        title = r.get(JobFields.TITLE).stringValue,
                        description =
                                if (r.get(JobFields.DESCRIPTION).isNull) null
                                else r.get(JobFields.DESCRIPTION).stringValue,
                        seniorityLevel = r.get(JobFields.SENIORITY_LEVEL).stringValue,
                        employmentType =
                                if (r.get(JobFields.EMPLOYMENT_TYPE).isNull) null
                                else r.get(JobFields.EMPLOYMENT_TYPE).stringValue,
                        workModel =
                                if (r.get(JobFields.WORK_MODEL).isNull) null
                                else r.get(JobFields.WORK_MODEL).stringValue,
                        postedDate =
                                if (r.get(JobFields.POSTED_DATE).isNull) null
                                else
                                        java.time.LocalDate.parse(
                                                r.get(JobFields.POSTED_DATE).stringValue
                                        ),
                        jobFunction =
                                if (r.get(JobFields.JOB_FUNCTION).isNull) null
                                else r.get(JobFields.JOB_FUNCTION).stringValue,
                        technologies = techList,
                        benefits = benefitList
                )

        val locations = mutableListOf<com.techmarket.api.model.JobLocationDto>()
        if (!r.get(JobFields.LOCATIONS).isNull && !r.get(JobFields.JOB_IDS).isNull) {
            val locArr = r.get(JobFields.LOCATIONS).repeatedValue
            val idArr = r.get(JobFields.JOB_IDS).repeatedValue
            val applyArr =
                    if (r.get(JobFields.APPLY_URLS).isNull) emptyList()
                    else r.get(JobFields.APPLY_URLS).repeatedValue

            for (i in 0 until min(locArr.size, idArr.size)) {
                locations.add(
                        com.techmarket.api.model.JobLocationDto(
                                location = locArr[i].stringValue,
                                jobId = idArr[i].stringValue,
                                applyUrl =
                                        if (i < applyArr.size && !applyArr[i].isNull)
                                                applyArr[i].stringValue
                                        else null
                        )
                )
            }
        }

        val hiringLocations =
                if (r.get("comp_hiringLocations").isNull) emptyList<String>()
                else r.get("comp_hiringLocations").repeatedValue.map { it.stringValue }

        val company =
                com.techmarket.api.model.JobCompanyDto(
                        companyId = r.get(JobFields.COMPANY_ID).stringValue,
                        name = r.get("comp_name").stringValue,
                        logoUrl =
                                if (r.get("comp_logo").isNull) ""
                                else r.get("comp_logo").stringValue,
                        description =
                                if (r.get("comp_desc").isNull) ""
                                else r.get("comp_desc").stringValue,
                        website =
                                if (r.get("comp_web").isNull) "" else r.get("comp_web").stringValue,
                        hiringLocations = hiringLocations
                )

        val similarRoles =
                similarResult.values.map { sim ->
                    val simTechList =
                            if (sim.get(JobFields.TECHNOLOGIES).isNull) emptyList<String>()
                            else
                                    sim.get(JobFields.TECHNOLOGIES).repeatedValue.map {
                                        it.stringValue
                                    }
                    val simLocList =
                            if (sim.get(JobFields.LOCATIONS).isNull) emptyList<String>()
                            else sim.get(JobFields.LOCATIONS).repeatedValue.map { it.stringValue }
                    val simIdList =
                            if (sim.get(JobFields.JOB_IDS).isNull) emptyList<String>()
                            else sim.get(JobFields.JOB_IDS).repeatedValue.map { it.stringValue }
                    val simApplyList =
                            if (sim.get(JobFields.APPLY_URLS).isNull) emptyList<String?>()
                            else
                                    sim.get(JobFields.APPLY_URLS).repeatedValue.map {
                                        if (it.isNull) null else it.stringValue
                                    }

                    JobRoleDto(
                            id = simIdList.firstOrNull() ?: "",
                            title = sim.get(JobFields.TITLE).stringValue,
                            companyId = sim.get(JobFields.COMPANY_ID).stringValue,
                            companyName =
                                    if (sim.get(JobFields.COMPANY_NAME).isNull) ""
                                    else sim.get(JobFields.COMPANY_NAME).stringValue,
                            locations = simLocList,
                            jobIds = simIdList,
                            applyUrls = simApplyList,
                            salaryMin =
                                    if (sim.get(JobFields.SALARY_MIN).isNull) null
                                    else sim.get(JobFields.SALARY_MIN).longValue.toInt(),
                            salaryMax =
                                    if (sim.get(JobFields.SALARY_MAX).isNull) null
                                    else sim.get(JobFields.SALARY_MAX).longValue.toInt(),
                            postedDate =
                                    if (sim.get(JobFields.POSTED_DATE).isNull) ""
                                    else sim.get(JobFields.POSTED_DATE).stringValue,
                            technologies = simTechList
                    )
                }

        return com.techmarket.api.model.JobPageDto(
                details = details,
                locations = locations,
                company = company,
                similarRoles = similarRoles
        )
    }
}
