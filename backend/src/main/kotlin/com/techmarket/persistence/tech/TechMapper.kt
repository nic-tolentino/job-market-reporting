package com.techmarket.persistence.tech

import com.techmarket.api.model.CompanyLeaderboardDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.api.model.SeniorityDistributionDto
import com.techmarket.api.model.TechDetailsPageDto
import com.techmarket.models.CompanyLeaderboardRow
import com.techmarket.models.JobRow
import com.techmarket.models.SeniorityRow
import com.techmarket.persistence.JobRowMapper.mapToJobRole
import com.techmarket.util.TechFormatter

object TechMapper {
    /**
     * Maps typed row objects to TechDetailsPageDto.
     * All raw FieldValueList access is handled in QueryRows.kt
     */
    fun mapTechDetails(
        techName: String,
        seniorityRows: List<SeniorityRow>,
        companyRows: List<CompanyLeaderboardRow>,
        jobRows: List<JobRow>
    ): TechDetailsPageDto {

        val seniorityDistribution = seniorityRows.map { mapSeniorityLine(it) }
        val companies = companyRows.map { mapCompanyLine(it) }
        val roles = jobRows.map { mapToJobRole(it) }

        val totalJobs = seniorityDistribution.sumOf { it.value }

        return TechDetailsPageDto(
            techName = TechFormatter.format(techName),
            totalJobs,
            seniorityDistribution,
            companies,
            roles
        )
    }

    fun mapSeniorityLine(row: SeniorityRow): SeniorityDistributionDto {
        return SeniorityDistributionDto(
            row.name,
            row.value
        )
    }

    fun mapCompanyLine(row: CompanyLeaderboardRow): CompanyLeaderboardDto {
        return CompanyLeaderboardDto(
            id = row.id,
            name = row.name,
            logo = row.logo,
            activeRoles = row.activeRoles
        )
    }
}
