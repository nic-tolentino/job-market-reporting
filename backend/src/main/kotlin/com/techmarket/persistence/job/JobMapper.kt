package com.techmarket.persistence.job

import com.techmarket.api.model.JobPageDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.CompanyAliases
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.SalaryMapper
import com.techmarket.persistence.model.JobRecord
import com.techmarket.util.LocationFormatter
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
        ): JobPageDto {
                return JobPageDto(
                        details = mapJobDetailsDto(r, techList),
                        locations = mapJobLocations(r),
                        company = mapJobCompanyDto(r),
                        similarRoles = similarResult.values.map { mapJobRole(it) }
                )
        }

        fun mapJobDetailsDto(r: com.google.cloud.bigquery.FieldValueList, techList: List<String>): com.techmarket.api.model.JobDetailsDto {
                val benefitList =
                        if (r.get(JobFields.BENEFITS).isNull) null
                        else r.get(JobFields.BENEFITS).repeatedValue.map { it.stringValue }

                return com.techmarket.api.model.JobDetailsDto(
                        title = r.get(JobFields.TITLE).stringValue,
                        description = if (r.get(JobFields.DESCRIPTION).isNull) null else r.get(JobFields.DESCRIPTION).stringValue,
                        seniorityLevel = r.get(JobFields.SENIORITY_LEVEL).stringValue,
                        employmentType = if (r.get(JobFields.EMPLOYMENT_TYPE).isNull) null else r.get(JobFields.EMPLOYMENT_TYPE).stringValue,
                        workModel = if (r.get(JobFields.WORK_MODEL).isNull) null else r.get(JobFields.WORK_MODEL).stringValue,
                        postedDate = if (r.get(JobFields.POSTED_DATE).isNull) null else parseLocalDateSafe(r.get(JobFields.POSTED_DATE)),
                        jobFunction = if (r.get(JobFields.JOB_FUNCTION).isNull) null else r.get(JobFields.JOB_FUNCTION).stringValue,
                        technologies = techList.map { TechFormatter.format(it) },
                        benefits = benefitList
                )
        }

        fun mapJobLocations(r: com.google.cloud.bigquery.FieldValueList): List<com.techmarket.api.model.JobLocationDto> {
                val locations = mutableListOf<com.techmarket.api.model.JobLocationDto>()
                if (!r.get(JobFields.LOCATIONS).isNull && !r.get(JobFields.JOB_IDS).isNull) {
                        val locArr = r.get(JobFields.LOCATIONS).repeatedValue
                        val idArr = r.get(JobFields.JOB_IDS).repeatedValue
                        val applyArr = if (r.get(JobFields.APPLY_URLS).isNull) emptyList() else r.get(JobFields.APPLY_URLS).repeatedValue
                        val linkArr = if (r.get(JobFields.PLATFORM_LINKS).isNull) emptyList() else r.get(JobFields.PLATFORM_LINKS).repeatedValue

                        for (i in 0 until min(locArr.size, idArr.size)) {
                                val rawLoc = locArr[i].stringValue
                                locations.add(
                                        com.techmarket.api.model.JobLocationDto(
                                                location = LocationFormatter.format(rawLoc),
                                                jobId = idArr[i].stringValue,
                                                applyUrl = if (i < applyArr.size && !applyArr[i].isNull && applyArr[i].stringValue.isNotBlank()) applyArr[i].stringValue else null,
                                                link = if (i < linkArr.size && !linkArr[i].isNull && linkArr[i].stringValue.isNotBlank()) linkArr[i].stringValue else null
                                        )
                                )
                        }
                }
                return locations
        }

        fun mapJobCompanyDto(r: com.google.cloud.bigquery.FieldValueList): com.techmarket.api.model.JobCompanyDto {
                val hiringLocations =
                        if (r.get(CompanyAliases.HIRING_LOCATIONS).isNull) emptyList<String>()
                        else
                                r.get(CompanyAliases.HIRING_LOCATIONS).repeatedValue.map {
                                        LocationFormatter.format(it.stringValue)
                                }

                return com.techmarket.api.model.JobCompanyDto(
                        companyId = r.get(JobFields.COMPANY_ID).stringValue,
                        name = if (r.get(CompanyAliases.NAME).isNull) "Unknown Company" else r.get(CompanyAliases.NAME).stringValue,
                        logoUrl = if (r.get(CompanyAliases.LOGO_URL).isNull) "" else r.get(CompanyAliases.LOGO_URL).stringValue,
                        description = if (r.get(CompanyAliases.DESCRIPTION).isNull) "" else r.get(CompanyAliases.DESCRIPTION).stringValue,
                        website = if (r.get(CompanyAliases.WEBSITE).isNull) "" else r.get(CompanyAliases.WEBSITE).stringValue,
                        hiringLocations = hiringLocations,
                        hqCountry = if (r.get(CompanyAliases.HQ_COUNTRY).isNull) null else r.get(CompanyAliases.HQ_COUNTRY).stringValue,
                        verificationLevel = if (r.get(CompanyAliases.VERIFICATION_LEVEL).isNull) "unverified" else r.get(CompanyAliases.VERIFICATION_LEVEL).stringValue
                )
        }

        fun mapJobRole(row: com.google.cloud.bigquery.FieldValueList): JobRoleDto {
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
                                if (stateRegion == "Unknown" || stateRegion == city)
                                        city
                                else "$city, $stateRegion"
                        )
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
                        if (row.get(JobFields.PLATFORM_LINKS).isNull)
                                emptyList<String?>()
                        else
                                row.get(JobFields.PLATFORM_LINKS)
                                        .repeatedValue
                                        .map {
                                                if (it.isNull) null
                                                else it.stringValue
                                        }

                val source =
                        if (row.get(JobFields.SOURCE).isNull) "Unknown"
                        else row.get(JobFields.SOURCE).stringValue
                val country =
                        if (row.get(JobFields.COUNTRY).isNull) null
                        else row.get(JobFields.COUNTRY).stringValue
                val lastUpdatedAt =
                        if (row.get(JobFields.LAST_SEEN_AT).isNull) Instant.EPOCH
                        else parseTimestampSafe(row.get(JobFields.LAST_SEEN_AT))

                return JobRoleDto(
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
                        salaryMin = SalaryMapper.fromFieldValue(row, JobFields.SALARY_MIN, country),
                        salaryMax = SalaryMapper.fromFieldValue(row, JobFields.SALARY_MAX, country),
                        postedDate =
                                if (row.get(JobFields.POSTED_DATE).isNull) ""
                                else row.get(JobFields.POSTED_DATE).stringValue,
                        seniorityLevel =
                                row.get(JobFields.SENIORITY_LEVEL).stringValue,
                        technologies = techList,
                        source = source,
                        lastUpdatedAt = lastUpdatedAt
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
                        salaryMin = SalaryMapper.fromFieldValue(r, JobFields.SALARY_MIN, r.get(JobFields.COUNTRY).stringValue),
                        salaryMax = SalaryMapper.fromFieldValue(r, JobFields.SALARY_MAX, r.get(JobFields.COUNTRY).stringValue),
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
