package com.techmarket.persistence.company

import com.techmarket.api.model.CompanyDetailsDto
import com.techmarket.api.model.CompanyInsightsDto
import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.util.TechFormatter
import com.techmarket.persistence.model.VerificationLevel
import java.time.Instant

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
                val description =
                        if (detRow?.get(CompanyFields.DESCRIPTION)?.isNull == false)
                                detRow.get(CompanyFields.DESCRIPTION).stringValue
                        else ""
                val isAgency =
                        if (detRow?.get(CompanyFields.IS_AGENCY)?.isNull == false)
                                detRow.get(CompanyFields.IS_AGENCY).booleanValue
                        else false
                val isSocio =
                        if (detRow?.get(CompanyFields.IS_SOCIAL_ENTERPRISE)?.isNull == false)
                                detRow.get(CompanyFields.IS_SOCIAL_ENTERPRISE).booleanValue
                        else false
                val hq =
                        if (detRow?.get(CompanyFields.HQ_COUNTRY)?.isNull == false)
                                detRow.get(CompanyFields.HQ_COUNTRY).stringValue
                        else null
                val remotePlc =
                        if (detRow?.get(CompanyFields.REMOTE_POLICY)?.isNull == false)
                                detRow.get(CompanyFields.REMOTE_POLICY).stringValue
                        else null
                val visaSpons =
                        if (detRow?.get(CompanyFields.VISA_SPONSORSHIP)?.isNull == false)
                                detRow.get(CompanyFields.VISA_SPONSORSHIP).booleanValue
                        else false
                val verifLevel =
                        if (detRow?.get(CompanyFields.VERIFICATION_LEVEL)?.isNull == false)
                                VerificationLevel.fromString(detRow.get(CompanyFields.VERIFICATION_LEVEL).stringValue)
                        else VerificationLevel.VERIFIED

                val details =
                        CompanyDetailsDto(
                                companyId,
                                name,
                                logo,
                                website,
                                emps,
                                ind,
                                description,
                                isAgency,
                                isSocio,
                                hq,
                                remotePlc,
                                visaSpons,
                                verifLevel
                        )

                val roles =
                        jobsResult.values.map { r ->
                                // ... (roles aggregation same as before)
                                val techList =
                                        if (r.get(JobFields.TECHNOLOGIES).isNull)
                                                emptyList<String>()
                                        else
                                                r.get(JobFields.TECHNOLOGIES).repeatedValue.map {
                                                        TechFormatter.format(it.stringValue)
                                                }
                                val city =
                                        if (r.get(JobFields.CITY).isNull) "Unknown"
                                        else r.get(JobFields.CITY).stringValue
                                val stateRegion =
                                        if (r.get(JobFields.STATE_REGION).isNull) "Unknown"
                                        else r.get(JobFields.STATE_REGION).stringValue
                                val locationList =
                                        listOf(
                                                if (stateRegion == "Unknown" || stateRegion == city)
                                                        city
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
                                val linkList =
                                        if (r.get(JobFields.PLATFORM_LINKS).isNull)
                                                emptyList<String?>()
                                        else
                                                r.get(JobFields.PLATFORM_LINKS).repeatedValue.map {
                                                        if (it.isNull) null else it.stringValue
                                                }
                                JobRoleDto(
                                        id = r.get(JobFields.JOB_ID).stringValue,
                                        title = r.get(JobFields.TITLE).stringValue,
                                        companyId = companyId,
                                        companyName = name,
                                        locations = locationList,
                                        jobIds = jobIdList,
                                        applyUrls = applyUrlList,
                                        platformLinks = linkList,
                                        salaryMin =
                                                if (r.get(JobFields.SALARY_MIN).isNull) null
                                                else r.get(JobFields.SALARY_MIN).longValue.toInt(),
                                        salaryMax =
                                                if (r.get(JobFields.SALARY_MAX).isNull) null
                                                else r.get(JobFields.SALARY_MAX).longValue.toInt(),
                                        postedDate =
                                                if (r.get(JobFields.POSTED_DATE).isNull) ""
                                                else r.get(JobFields.POSTED_DATE).stringValue,
                                        seniorityLevel =
                                                r.get(JobFields.SENIORITY_LEVEL).stringValue,
                                        technologies = techList
                                )
                        }

                val allTechs =
                        if (detRow?.get(CompanyFields.TECHNOLOGIES)?.isNull == false)
                                detRow.get(CompanyFields.TECHNOLOGIES).repeatedValue.map {
                                        TechFormatter.format(it.stringValue)
                                }
                        else emptyList()
                val hiringLocations =
                        if (detRow?.get(CompanyFields.HIRING_LOCATIONS)?.isNull == false)
                                detRow.get(CompanyFields.HIRING_LOCATIONS).repeatedValue.map {
                                        val rawLoc = it.stringValue
                                        val parts = rawLoc.split(", ")
                                        if (parts.size == 2 && parts[0] == parts[1]) parts[0]
                                        else rawLoc
                                }
                        else emptyList()
                val operatingCountries =
                        if (detRow?.get(CompanyFields.OPERATING_COUNTRIES)?.isNull == false)
                                detRow.get(CompanyFields.OPERATING_COUNTRIES).repeatedValue.map {
                                        it.stringValue
                                }
                        else emptyList()
                val officeLocations =
                        if (detRow?.get(CompanyFields.OFFICE_LOCATIONS)?.isNull == false)
                                detRow.get(CompanyFields.OFFICE_LOCATIONS).repeatedValue.map {
                                        it.stringValue
                                }
                        else emptyList()

                val aggRow = aggResult.values.firstOrNull()
                val topModel =
                        try {
                                if (aggRow != null && !aggRow.get("topModel").isNull)
                                        aggRow.get("topModel").stringValue
                                else "Hybrid Friendly"
                        } catch (e: Exception) {
                                "Hybrid Friendly"
                        }

                val allBenefits =
                        jobsResult
                                .values
                                .mapNotNull { r ->
                                        try {
                                                if (r.get(JobFields.BENEFITS).isNull) null
                                                else
                                                        r.get(JobFields.BENEFITS)
                                                                .repeatedValue
                                                                .map { it.stringValue }
                                        } catch (e: Exception) {
                                                null
                                        }
                                }
                                .flatten()
                                .groupingBy { it }
                                .eachCount()
                                .entries
                                .sortedByDescending { it.value }
                                .map { it.key }
                                .take(5)

                val insights =
                        CompanyInsightsDto(
                                topModel,
                                hiringLocations,
                                allBenefits,
                                operatingCountries,
                                officeLocations
                        )

                return CompanyProfilePageDto(details, allTechs, insights, roles)
        }

        fun mapToCompanyRecord(row: com.google.cloud.bigquery.FieldValueList): CompanyRecord {
                return CompanyRecord(
                        companyId = row.get(CompanyFields.COMPANY_ID).stringValue,
                        name = row.get(CompanyFields.NAME).stringValue,
                        alternateNames =
                                if (row.get(CompanyFields.ALTERNATE_NAMES).isNull) emptyList()
                                else
                                        row.get(CompanyFields.ALTERNATE_NAMES).repeatedValue.map {
                                                it.stringValue
                                        },
                        logoUrl =
                                if (row.get(CompanyFields.LOGO_URL).isNull) null
                                else row.get(CompanyFields.LOGO_URL).stringValue,
                        description =
                                if (row.get(CompanyFields.DESCRIPTION).isNull) null
                                else row.get(CompanyFields.DESCRIPTION).stringValue,
                        website =
                                if (row.get(CompanyFields.WEBSITE).isNull) null
                                else row.get(CompanyFields.WEBSITE).stringValue,
                        employeesCount =
                                if (row.get(CompanyFields.EMPLOYEES_COUNT).isNull) null
                                else row.get(CompanyFields.EMPLOYEES_COUNT).longValue.toInt(),
                        industries =
                                if (row.get(CompanyFields.INDUSTRIES).isNull) null
                                else row.get(CompanyFields.INDUSTRIES).stringValue,
                        technologies =
                                if (row.get(CompanyFields.TECHNOLOGIES).isNull) emptyList()
                                else
                                        row.get(CompanyFields.TECHNOLOGIES).repeatedValue.map {
                                                it.stringValue
                                        },
                        hiringLocations =
                                if (row.get(CompanyFields.HIRING_LOCATIONS).isNull) emptyList()
                                else
                                        row.get(CompanyFields.HIRING_LOCATIONS).repeatedValue.map {
                                                it.stringValue
                                        },
                        isAgency =
                                if (row.get(CompanyFields.IS_AGENCY).isNull) false
                                else row.get(CompanyFields.IS_AGENCY).booleanValue,
                        isSocialEnterprise =
                                if (row.get(CompanyFields.IS_SOCIAL_ENTERPRISE).isNull) false
                                else row.get(CompanyFields.IS_SOCIAL_ENTERPRISE).booleanValue,
                        hqCountry =
                                if (row.get(CompanyFields.HQ_COUNTRY).isNull) null
                                else row.get(CompanyFields.HQ_COUNTRY).stringValue,
                        operatingCountries =
                                if (row.get(CompanyFields.OPERATING_COUNTRIES).isNull) emptyList()
                                else
                                        row.get(CompanyFields.OPERATING_COUNTRIES)
                                                .repeatedValue
                                                .map { it.stringValue },
                        officeLocations =
                                if (row.get(CompanyFields.OFFICE_LOCATIONS).isNull) emptyList()
                                else
                                        row.get(CompanyFields.OFFICE_LOCATIONS).repeatedValue.map {
                                                it.stringValue
                                        },
                        remotePolicy =
                                if (row.get(CompanyFields.REMOTE_POLICY).isNull) null
                                else row.get(CompanyFields.REMOTE_POLICY).stringValue,
                        visaSponsorship =
                                if (row.get(CompanyFields.VISA_SPONSORSHIP).isNull) false
                                else row.get(CompanyFields.VISA_SPONSORSHIP).booleanValue,
                        verificationLevel =
                                if (row.get(CompanyFields.VERIFICATION_LEVEL).isNull) VerificationLevel.VERIFIED
                                else VerificationLevel.fromString(row.get(CompanyFields.VERIFICATION_LEVEL).stringValue),
                        lastUpdatedAt =
                                Instant.parse(row.get(CompanyFields.LAST_UPDATED_AT).stringValue)
                )
        }
}
