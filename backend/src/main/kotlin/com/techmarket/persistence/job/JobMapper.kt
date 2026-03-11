package com.techmarket.persistence.job

import com.techmarket.api.model.JobPageDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.models.CompanyInfoRow
import com.techmarket.models.JobDetailsRow
import com.techmarket.models.JobRow
import com.techmarket.persistence.JobRowMapper.buildLocationList
import com.techmarket.persistence.JobRowMapper.mapToJobLocations
import com.techmarket.persistence.JobRowMapper.mapToJobRole
import com.techmarket.util.TechFormatter
import java.time.LocalDate

object JobMapper {
    /**
     * Maps a typed JobDetailsRow to JobPageDto.
     * All raw FieldValueList access is handled in QueryRows.kt
     */
    fun mapJobDetails(
        detailsRow: JobDetailsRow,
        similarResult: List<JobRow>
    ): JobPageDto {
        return JobPageDto(
            details = mapJobDetailsDto(detailsRow.job),
            locations = mapToJobLocations(detailsRow.job),
            company = mapJobCompanyDto(detailsRow.company),
            similarRoles = similarResult.map { mapToJobRole(it) }
        )
    }

    fun mapJobDetailsDto(job: JobRow): com.techmarket.api.model.JobDetailsDto {
        return com.techmarket.api.model.JobDetailsDto(
            title = job.title,
            description = job.description,
            seniorityLevel = job.seniorityLevel,
            employmentType = job.employmentType,
            workModel = job.workModel,
            postedDate = job.postedDate.ifEmpty { null }?.let { LocalDate.parse(it) },
            jobFunction = job.jobFunction,
            technologies = job.technologies.map { TechFormatter.format(it) },
            benefits = job.benefits.ifEmpty { null }
        )
    }

    fun mapJobCompanyDto(company: CompanyInfoRow): com.techmarket.api.model.JobCompanyDto {
        val hiringLocations = company.hiringLocations
            .map { com.techmarket.util.LocationFormatter.format(it) }

        return com.techmarket.api.model.JobCompanyDto(
            companyId = company.companyId,
            name = company.name,
            logoUrl = company.logoUrl,
            description = company.description,
            website = company.website,
            hiringLocations = hiringLocations,
            hqCountry = company.hqCountry,
            verificationLevel = company.verificationLevel
        )
    }

    fun mapJobRole(job: JobRow): JobRoleDto {
        return mapToJobRole(job)
    }

    fun mapToJobRecord(job: JobRow): com.techmarket.persistence.model.JobRecord {
        return com.techmarket.persistence.model.JobRecord(
            jobId = job.jobId,
            platformJobIds = job.jobIds,
            applyUrls = job.applyUrls.filterNotNull(),
            platformLinks = job.platformLinks.filterNotNull(),
            locations = job.locations,
            companyId = job.companyId,
            companyName = job.companyName,
            source = job.source,
            country = job.country ?: "unknown",
            city = job.city,
            stateRegion = job.stateRegion,
            title = job.title,
            seniorityLevel = job.seniorityLevel,
            technologies = job.technologies,
            salaryMin = job.salaryMin,
            salaryMax = job.salaryMax,
            postedDate = job.postedDate.ifEmpty { null }?.let { LocalDate.parse(it) },
            benefits = job.benefits,
            employmentType = job.employmentType,
            workModel = job.workModel,
            jobFunction = job.jobFunction,
            description = job.description,
            lastSeenAt = job.lastSeenAt
        )
    }
}
