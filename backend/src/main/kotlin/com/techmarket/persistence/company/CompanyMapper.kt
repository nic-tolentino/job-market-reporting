package com.techmarket.persistence.company

import com.techmarket.api.model.CompanyDetailsDto
import com.techmarket.api.model.CompanyInsightsDto
import com.techmarket.api.model.CompanyProfilePageDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.models.CompanyRow
import com.techmarket.models.JobRow
import com.techmarket.models.VisaSponsorshipInfo
import com.techmarket.persistence.CommonLiterals.HYBRID_FRIENDLY
import com.techmarket.persistence.CommonLiterals.UNKNOWN
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.VerificationLevel
import com.techmarket.util.TechFormatter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory

/**
 * Maps typed row objects to DTOs.
 */
object CompanyMapper {
    private val log = LoggerFactory.getLogger(CompanyMapper::class.java)
    private val mapper = jacksonObjectMapper()
    
    fun mapCompanyProfile(
        companyId: String,
        companyRow: CompanyRow,
        jobRows: List<JobRow>,
        topModel: String?
    ): CompanyProfilePageDto {
        val details = mapCompanyDetails(companyId, companyRow)
        
        val techFromJobs = jobRows
            .flatMap { it.technologies }
            .map { TechFormatter.format(it) }
            .distinct()
            .sorted()
        
        val roles = jobRows.map { job ->
            mapJobRole(job, companyRow.name, companyId)
        }
        
        val companyTechs = companyRow.technologies.map { TechFormatter.format(it) }
        val allTechs = (companyTechs + techFromJobs).distinct().sorted()
        
        val insights = mapCompanyInsights(companyRow, jobRows, topModel)
        
        return CompanyProfilePageDto(details, allTechs, insights, roles)
    }
    
    private fun mapCompanyDetails(
        companyId: String,
        row: CompanyRow
    ): CompanyDetailsDto {
        return CompanyDetailsDto(
            id = companyId,
            name = row.name,
            logo = row.logoUrl ?: "",
            website = row.website ?: "",
            employeesCount = row.employeesCount ?: 0,
            industry = row.industries ?: "",
            description = row.description ?: "",
            isAgency = row.isAgency,
            isSocialEnterprise = row.isSocialEnterprise,
            hqCountry = row.hqCountry,
            remotePolicy = row.remotePolicy,
            visaSponsorship = getVisaSponsorshipInfoFallback(row.visaSponsorship, row.visaSponsorshipDetail),
            verificationLevel = VerificationLevel.fromString(row.verificationLevel)
        )
    }
    
    private fun mapJobRole(
        job: JobRow,
        companyName: String,
        companyId: String
    ): JobRoleDto {
        val locationList = buildLocationList(job.city, job.stateRegion)
        
        return JobRoleDto(
            id = job.jobId,
            title = job.title,
            companyId = companyId,
            companyName = companyName,
            locations = locationList,
            jobIds = job.jobIds,
            applyUrls = job.applyUrls,
            platformLinks = job.platformLinks,
            salaryMin = job.salaryMin,
            salaryMax = job.salaryMax,
            postedDate = job.postedDate,
            seniorityLevel = job.seniorityLevel,
            technologies = job.technologies.map { TechFormatter.format(it) },
            source = job.source,
            lastUpdatedAt = job.lastSeenAt
        )
    }
    
    private fun buildLocationList(city: String, stateRegion: String): List<String> {
        return listOf(
            if (stateRegion == UNKNOWN || stateRegion == city) city
            else "$city, $stateRegion"
        )
    }

    private fun mapCompanyInsights(
        company: CompanyRow,
        jobs: List<JobRow>,
        topModel: String?
    ): CompanyInsightsDto {
        val commonBenefits = jobs
            .flatMap { it.benefits }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(5)

        return CompanyInsightsDto(
            workModel = topModel ?: HYBRID_FRIENDLY,
            hiringLocations = company.hiringLocations,
            commonBenefits = commonBenefits,
            operatingCountries = company.operatingCountries,
            officeLocations = company.officeLocations
        )
    }
    
    fun mapToCompanyRecord(row: CompanyRow): CompanyRecord {
        return CompanyRecord(
            companyId = row.companyId,
            name = row.name,
            alternateNames = row.alternateNames,
            logoUrl = row.logoUrl,
            description = row.description,
            website = row.website,
            employeesCount = row.employeesCount,
            industries = row.industries,
            technologies = row.technologies,
            hiringLocations = row.hiringLocations,
            isAgency = row.isAgency,
            isSocialEnterprise = row.isSocialEnterprise,
            hqCountry = row.hqCountry,
            operatingCountries = row.operatingCountries,
            officeLocations = row.officeLocations,
            remotePolicy = row.remotePolicy,
            visaSponsorship = getVisaSponsorshipInfoFallback(row.visaSponsorship, row.visaSponsorshipDetail),
            verificationLevel = VerificationLevel.fromString(row.verificationLevel),
            lastUpdatedAt = row.lastUpdatedAt
        )
    }
    
    private fun getVisaSponsorshipInfoFallback(legacySponsorship: Boolean, detailedSponsorshipJson: String?): VisaSponsorshipInfo? {
        if (detailedSponsorshipJson != null) {
            try {
                return mapper.readValue(detailedSponsorshipJson, VisaSponsorshipInfo::class.java)
            } catch (e: Exception) {
                log.warn("Failed to parse visa_sponsorship_detail JSON: ${e.message}")
            }
        }
        return if (legacySponsorship) VisaSponsorshipInfo(offered = true) else null
    }
}
