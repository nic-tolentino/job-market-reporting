package com.techmarket.sync

import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.persistence.model.JobRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Responsible for merging newly mapped records with existing data in the Silver layer. This ensures
 * the "Current State" is preserved and extended while avoiding duplicates.
 */
@Service
class SilverDataMerger {

    private val log = LoggerFactory.getLogger(SilverDataMerger::class.java)

    /** Merges a batch of new job records with their existing counterparts. */
    fun mergeJobs(newJobs: List<JobRecord>, existingJobs: List<JobRecord>): List<JobRecord> {
        val existingMap = existingJobs.associateBy { it.jobId }

        return newJobs.map { newJob ->
            val existing = existingMap[newJob.jobId]
            if (existing == null) {
                newJob
            } else {
                mergeJobRecord(newJob, existing)
            }
        }
    }

    /** Merges a batch of new company records with their existing counterparts. */
    fun mergeCompanies(
            newCompanies: List<CompanyRecord>,
            existingCompanies: List<CompanyRecord>
    ): List<CompanyRecord> {
        val existingMap = existingCompanies.associateBy { it.companyId }

        return newCompanies.map { newCompany ->
            val existing = existingMap[newCompany.companyId]
            if (existing == null) {
                newCompany
            } else {
                mergeCompanyRecord(newCompany, existing)
            }
        }
    }

    private fun mergeJobRecord(new: JobRecord, existing: JobRecord): JobRecord {
        // We always assume 'new' is the latest snapshot from the perspective of the current sync
        val latest = if (new.lastSeenAt.isAfter(existing.lastSeenAt)) new else existing
        val oldest = if (new.lastSeenAt.isBefore(existing.lastSeenAt)) new else existing

        return latest.copy(
                // 1. Durations & Lifecycle
                postedDate = minOfNullable(new.postedDate, existing.postedDate),
                lastSeenAt = maxOf(new.lastSeenAt, existing.lastSeenAt),

                // 2. List Aggregators (Union + Distinct + Sorted)
                platformJobIds = (new.platformJobIds + existing.platformJobIds).distinct().sorted(),
                applyUrls = (new.applyUrls + existing.applyUrls).distinct().sorted(),
                platformLinks = (new.platformLinks + existing.platformLinks).distinct().sorted(),
                locations = (new.locations + existing.locations).distinct().sorted(),
                technologies = (new.technologies + existing.technologies).distinct().sorted(),
                benefits = (new.benefits + existing.benefits).distinct().sorted(),

                // 3. Metadata: if latest is sparse, maybe some fields from oldest are better?
                // (Keeping it simple: latest wins for text fields except for the aggregators)
                description = new.description ?: existing.description,
                salaryMin = minOfNullableInt(new.salaryMin, existing.salaryMin),
                salaryMax = maxOfNullableInt(new.salaryMax, existing.salaryMax)
        )
    }

    private fun mergeCompanyRecord(new: CompanyRecord, existing: CompanyRecord): CompanyRecord {
        return existing.copy(
                // Metadata Updates: Take latest non-blank
                name = new.name.ifBlank { existing.name },
                alternateNames = (new.alternateNames + existing.alternateNames).distinct().sorted(),
                logoUrl = new.logoUrl?.ifBlank { null } ?: existing.logoUrl,
                description = new.description?.ifBlank { null } ?: existing.description,
                website = new.website?.ifBlank { null } ?: existing.website,
                employeesCount = new.employeesCount ?: existing.employeesCount,
                industries = new.industries?.ifBlank { null } ?: existing.industries,

                // Aggregators
                technologies = (new.technologies + existing.technologies).distinct().sorted(),
                hiringLocations =
                        (new.hiringLocations + existing.hiringLocations).distinct().sorted(),

                // Timestamp
                lastUpdatedAt = maxOf(new.lastUpdatedAt, existing.lastUpdatedAt)
        )
    }

    private fun <T : Comparable<T>> minOfNullable(a: T?, b: T?): T? {
        return when {
            a == null -> b
            b == null -> a
            else -> if (a < b) a else b
        }
    }

    private fun minOfNullableInt(a: Int?, b: Int?): Int? {
        return when {
            a == null -> b
            b == null -> a
            else -> kotlin.math.min(a, b)
        }
    }

    private fun maxOfNullableInt(a: Int?, b: Int?): Int? {
        return when {
            a == null -> b
            b == null -> a
            else -> kotlin.math.max(a, b)
        }
    }
}
