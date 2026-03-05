package com.techmarket.persistence.job

import com.techmarket.api.model.JobRoleDto
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.model.JobRecord
import com.techmarket.util.TechFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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
                                        if (r.get(JobFields.POSTED_DATE).isNull) null
                                        else parseLocalDateSafe(r.get(JobFields.POSTED_DATE)),
                                jobFunction =
                                        if (r.get(JobFields.JOB_FUNCTION).isNull) null
                                        else r.get(JobFields.JOB_FUNCTION).stringValue,
                                technologies = techList.map { TechFormatter.format(it) },
                                benefits = benefitList
                        )

                val locations = mutableListOf<com.techmarket.api.model.JobLocationDto>()
                if (!r.get(JobFields.LOCATIONS).isNull && !r.get(JobFields.JOB_IDS).isNull) {
                        val locArr = r.get(JobFields.LOCATIONS).repeatedValue
                        val idArr = r.get(JobFields.JOB_IDS).repeatedValue
                        val applyArr =
                                if (r.get(JobFields.APPLY_URLS).isNull) emptyList()
                                else r.get(JobFields.APPLY_URLS).repeatedValue
                        val linkArr =
                                if (r.get(JobFields.PLATFORM_LINKS).isNull) emptyList()
                                else r.get(JobFields.PLATFORM_LINKS).repeatedValue

                        for (i in 0 until min(locArr.size, idArr.size)) {
                                val rawLoc = locArr[i].stringValue
                                val locParts = rawLoc.split(", ")
                                val displayLoc =
                                        if (locParts.size == 2 && locParts[0] == locParts[1])
                                                locParts[0]
                                        else rawLoc

                                locations.add(
                                        com.techmarket.api.model.JobLocationDto(
                                                location = displayLoc,
                                                jobId = idArr[i].stringValue,
                                                applyUrl =
                                                        if (i < applyArr.size &&
                                                                        !applyArr[i].isNull &&
                                                                        applyArr[i].stringValue
                                                                                .isNotBlank()
                                                        )
                                                                applyArr[i].stringValue
                                                        else null,
                                                link =
                                                        if (i < linkArr.size &&
                                                                        !linkArr[i].isNull &&
                                                                        linkArr[i].stringValue
                                                                                .isNotBlank()
                                                        )
                                                                linkArr[i].stringValue
                                                        else null
                                        )
                                )
                        }
                }

                val hiringLocations =
                        if (r.get("comp_hiringLocations").isNull) emptyList<String>()
                        else
                                r.get("comp_hiringLocations").repeatedValue.map {
                                        val rawLoc = it.stringValue
                                        val parts = rawLoc.split(", ")
                                        if (parts.size == 2 && parts[0] == parts[1]) parts[0]
                                        else rawLoc
                                }

                val company =
                        com.techmarket.api.model.JobCompanyDto(
                                companyId = r.get(JobFields.COMPANY_ID).stringValue,
                                name =
                                        if (r.get("comp_name").isNull) "Unknown Company"
                                        else r.get("comp_name").stringValue,
                                logoUrl =
                                        if (r.get("comp_logo").isNull) ""
                                        else r.get("comp_logo").stringValue,
                                description =
                                        if (r.get("comp_desc").isNull) ""
                                        else r.get("comp_desc").stringValue,
                                website =
                                        if (r.get("comp_web").isNull) ""
                                        else r.get("comp_web").stringValue,
                                hiringLocations = hiringLocations
                        )

                val similarRoles =
                        similarResult.values.map { sim ->
                                val simTechList =
                                        if (sim.get(JobFields.TECHNOLOGIES).isNull)
                                                emptyList<String>()
                                        else
                                                sim.get(JobFields.TECHNOLOGIES).repeatedValue.map {
                                                        it.stringValue
                                                }
                                val city =
                                        if (sim.get(JobFields.CITY).isNull) "Unknown"
                                        else sim.get(JobFields.CITY).stringValue
                                val stateRegion =
                                        if (sim.get(JobFields.STATE_REGION).isNull) "Unknown"
                                        else sim.get(JobFields.STATE_REGION).stringValue
                                val simLocList =
                                        listOf(
                                                if (stateRegion == "Unknown" || stateRegion == city)
                                                        city
                                                else "$city, $stateRegion"
                                        )
                                val simIdList =
                                        if (sim.get(JobFields.JOB_IDS).isNull) emptyList<String>()
                                        else
                                                sim.get(JobFields.JOB_IDS).repeatedValue.map {
                                                        it.stringValue
                                                }
                                val simApplyList =
                                        if (sim.get(JobFields.APPLY_URLS).isNull)
                                                emptyList<String?>()
                                        else
                                                sim.get(JobFields.APPLY_URLS).repeatedValue.map {
                                                        if (it.isNull) null else it.stringValue
                                                }
                                val simLinkList =
                                        if (sim.get(JobFields.PLATFORM_LINKS).isNull)
                                                emptyList<String?>()
                                        else
                                                sim.get(JobFields.PLATFORM_LINKS)
                                                        .repeatedValue
                                                        .map {
                                                                if (it.isNull) null
                                                                else it.stringValue
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
                                        platformLinks = simLinkList,
                                        salaryMin =
                                                if (sim.get(JobFields.SALARY_MIN).isNull) null
                                                else
                                                        sim.get(JobFields.SALARY_MIN)
                                                                .longValue
                                                                .toInt(),
                                        salaryMax =
                                                if (sim.get(JobFields.SALARY_MAX).isNull) null
                                                else
                                                        sim.get(JobFields.SALARY_MAX)
                                                                .longValue
                                                                .toInt(),
                                        postedDate =
                                                if (sim.get(JobFields.POSTED_DATE).isNull) ""
                                                else sim.get(JobFields.POSTED_DATE).stringValue,
                                        seniorityLevel =
                                                sim.get(JobFields.SENIORITY_LEVEL).stringValue,
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

        fun mapToJobRecord(r: com.google.cloud.bigquery.FieldValueList): JobRecord {
                return JobRecord(
                        jobId = r.get(JobFields.JOB_ID).stringValue,
                        platformJobIds =
                                if (r.get(JobFields.PLATFORM_JOB_IDS).isNull) emptyList()
                                else
                                        r.get(JobFields.PLATFORM_JOB_IDS).repeatedValue.map {
                                                it.stringValue
                                        },
                        applyUrls =
                                if (r.get(JobFields.APPLY_URLS).isNull) emptyList()
                                else
                                        r.get(JobFields.APPLY_URLS).repeatedValue.map {
                                                it.stringValue
                                        },
                        platformLinks =
                                if (r.get(JobFields.PLATFORM_LINKS).isNull) emptyList()
                                else
                                        r.get(JobFields.PLATFORM_LINKS).repeatedValue.map {
                                                it.stringValue
                                        },
                        locations =
                                if (r.get(JobFields.LOCATIONS).isNull) emptyList()
                                else
                                        r.get(JobFields.LOCATIONS).repeatedValue.map {
                                                it.stringValue
                                        },
                        companyId = r.get(JobFields.COMPANY_ID).stringValue,
                        companyName = r.get(JobFields.COMPANY_NAME).stringValue,
                        source = r.get(JobFields.SOURCE).stringValue,
                        country = r.get(JobFields.COUNTRY).stringValue,
                        city = r.get(JobFields.CITY).stringValue,
                        stateRegion = r.get(JobFields.STATE_REGION).stringValue,
                        title = r.get(JobFields.TITLE).stringValue,
                        seniorityLevel = r.get(JobFields.SENIORITY_LEVEL).stringValue,
                        technologies =
                                if (r.get(JobFields.TECHNOLOGIES).isNull) emptyList()
                                else
                                        r.get(JobFields.TECHNOLOGIES).repeatedValue.map {
                                                it.stringValue
                                        },
                        salaryMin =
                                if (r.get(JobFields.SALARY_MIN).isNull) null
                                else r.get(JobFields.SALARY_MIN).longValue.toInt(),
                        salaryMax =
                                if (r.get(JobFields.SALARY_MAX).isNull) null
                                else r.get(JobFields.SALARY_MAX).longValue.toInt(),
                        postedDate =
                                if (r.get(JobFields.POSTED_DATE).isNull) null
                                else parseLocalDateSafe(r.get(JobFields.POSTED_DATE)),
                        benefits =
                                if (r.get(JobFields.BENEFITS).isNull) emptyList()
                                else r.get(JobFields.BENEFITS).repeatedValue.map { it.stringValue },
                        employmentType =
                                if (r.get(JobFields.EMPLOYMENT_TYPE).isNull) null
                                else r.get(JobFields.EMPLOYMENT_TYPE).stringValue,
                        workModel =
                                if (r.get(JobFields.WORK_MODEL).isNull) null
                                else r.get(JobFields.WORK_MODEL).stringValue,
                        jobFunction =
                                if (r.get(JobFields.JOB_FUNCTION).isNull) null
                                else r.get(JobFields.JOB_FUNCTION).stringValue,
                        description =
                                if (r.get(JobFields.DESCRIPTION).isNull) null
                                else r.get(JobFields.DESCRIPTION).stringValue,
                        lastSeenAt = parseTimestampSafe(r.get(JobFields.LAST_SEEN_AT))
                )
        }

        private fun parseTimestampSafe(field: com.google.cloud.bigquery.FieldValue): Instant {
                if (field.isNull) return Instant.EPOCH
                return try {
                        val stringVal = field.stringValue
                        // Handle numeric strings like "1.772441037962E9"
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

        private fun parseLocalDateSafe(field: com.google.cloud.bigquery.FieldValue): LocalDate? {
                if (field.isNull) return null
                return try {
                        val stringVal = field.stringValue
                        val doubleVal = stringVal.toDoubleOrNull()
                        if (doubleVal != null) {
                                Instant.ofEpochSecond(doubleVal.toLong())
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                        } else {
                                LocalDate.parse(stringVal)
                        }
                } catch (e: Exception) {
                        null
                }
        }
}
