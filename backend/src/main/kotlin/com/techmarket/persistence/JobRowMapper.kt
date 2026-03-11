package com.techmarket.persistence

import com.techmarket.api.model.JobLocationDto
import com.techmarket.api.model.JobRoleDto
import com.techmarket.models.JobRow
import com.techmarket.util.LocationFormatter
import com.techmarket.util.TechFormatter
import java.time.Instant

/**
 * Shared mapper functions for converting JobRow to DTOs.
 * Prevents duplication across JobMapper and TechMapper.
 */
object JobRowMapper {

    /**
     * Maps a JobRow to JobRoleDto with consistent logic.
     */
    fun mapToJobRole(job: JobRow): JobRoleDto {
        return JobRoleDto(
            id = job.jobId,
            title = job.title,
            companyId = job.companyId,
            companyName = job.companyName,
            locations = buildLocationList(job.city, job.stateRegion),
            jobIds = job.jobIds,
            applyUrls = job.applyUrls.filterNotNull(),
            platformLinks = job.platformLinks.filterNotNull(),
            salaryMin = job.salaryMin,
            salaryMax = job.salaryMax,
            postedDate = job.postedDate,
            seniorityLevel = job.seniorityLevel,
            technologies = job.technologies.map { TechFormatter.format(it) },
            source = job.source,
            lastUpdatedAt = job.lastSeenAt
        )
    }

    /**
     * Builds a location list from city and stateRegion.
     * Handles the case where stateRegion equals city or is "Unknown".
     * 
     * Note: This function assumes city is non-empty as BigQuery job data always
     * includes a city value. Empty city strings result in formatting like ", Auckland"
     * which is a data quality issue that should be addressed at the ingestion level.
     */
    fun buildLocationList(city: String, stateRegion: String): List<String> {
        return listOf(
            if (stateRegion == "Unknown" || stateRegion == city) city
            else "$city, $stateRegion"
        )
    }

    /**
     * Maps JobRow locations to JobLocationDto list.
     */
    fun mapToJobLocations(job: JobRow): List<JobLocationDto> {
        val locations = mutableListOf<JobLocationDto>()
        val locArr = job.locations
        val idArr = job.jobIds
        val applyArr = job.applyUrls
        val linkArr = job.platformLinks

        if (locArr.isNotEmpty() && idArr.isNotEmpty()) {
            for (i in 0 until kotlin.math.min(locArr.size, idArr.size)) {
                val rawLoc = locArr[i]
                locations.add(
                    JobLocationDto(
                        location = LocationFormatter.format(rawLoc),
                        jobId = idArr[i],
                        applyUrl = applyArr.getOrNull(i),
                        link = linkArr.getOrNull(i)
                    )
                )
            }
        }
        return locations
    }
}
