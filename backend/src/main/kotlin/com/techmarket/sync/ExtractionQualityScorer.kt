package com.techmarket.sync

import com.techmarket.sync.ats.model.NormalizedJob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Calculates extraction quality scores for crawled jobs.
 * Used to identify companies needing prompt optimization.
 */
@Service
class ExtractionQualityScorer {
    private val log = LoggerFactory.getLogger(ExtractionQualityScorer::class.java)

    /**
     * Calculates overall quality score for a batch of extracted jobs.
     * Returns score between 0.0 and 1.0
     */
    fun calculateQualityScore(jobs: List<NormalizedJob>, previousJobsCount: Int? = null): QualityScore {
        if (jobs.isEmpty()) {
            return QualityScore(
                overall = 0.0,
                fieldCompletionRate = 0.0,
                consistencyScore = 0.0,
                anomalyDetected = true,
                anomalies = listOf("No jobs extracted")
            )
        }

        val anomalies = mutableListOf<String>()
        
        // 1. Field completion rate (40% weight)
        val fieldCompletionRate = calculateFieldCompletionRate(jobs)
        
        // 2. Consistency score (30% weight)
        val consistencyScore = calculateConsistencyScore(jobs)
        
        // 3. Job count anomaly detection (30% weight)
        val countScore = calculateCountScore(jobs.size, previousJobsCount, anomalies)
        
        // Weighted overall score
        val overall = (fieldCompletionRate * 0.4) + (consistencyScore * 0.3) + (countScore * 0.3)
        
        return QualityScore(
            overall = round(overall),
            fieldCompletionRate = round(fieldCompletionRate),
            consistencyScore = round(consistencyScore),
            countScore = round(countScore),
            anomalyDetected = anomalies.isNotEmpty(),
            anomalies = anomalies
        )
    }

    /**
     * Calculates field completion rate across all jobs.
     */
    private fun calculateFieldCompletionRate(jobs: List<NormalizedJob>): Double {
        val requiredFields = listOf(
            Pair("title", jobs.count { !it.title.isNullOrBlank() }),
            Pair("location", jobs.count { !it.location.isNullOrBlank() }),
            Pair("employmentType", jobs.count { !it.employmentType.isNullOrBlank() }),
            Pair("postedAt", jobs.count { !it.postedAt.isNullOrBlank() }),
            Pair("applyUrl", jobs.count { !it.applyUrl.isNullOrBlank() })
        )

        val totalPossible = requiredFields.size * jobs.size
        val totalPresent = requiredFields.sumOf { it.second }

        return totalPresent.toDouble() / totalPossible
    }

    /**
     * Calculates consistency score based on field value distributions.
     */
    private fun calculateConsistencyScore(jobs: List<NormalizedJob>): Double {
        var score = 1.0

        // Check employment type consistency
        val employmentTypes = jobs.mapNotNull { it.employmentType }.distinct()
        if (employmentTypes.size > 3) {
            score -= 0.1  // Too many different employment types
        }

        // Check work model consistency
        val workModels = jobs.mapNotNull { it.workModel }.distinct()
        if (workModels.size > 2) {
            score -= 0.1  // Too many different work models
        }

        // Check location format consistency
        val locations = jobs.mapNotNull { it.location }
        val hasCountryCode = locations.count { it.contains(Regex("\\b[A-Z]{2}\\b")) }
        if (locations.isNotEmpty() && hasCountryCode.toDouble() / locations.size < 0.5) {
            score -= 0.1  // Less than 50% have country codes
        }

        return maxOf(0.0, score)
    }

    /**
     * Calculates score based on job count vs previous crawl.
     */
    private fun calculateCountScore(
        currentCount: Int,
        previousCount: Int?,
        anomalies: MutableList<String>
    ): Double {
        if (previousCount == null) {
            return 1.0  // No previous data to compare
        }

        val change = currentCount - previousCount
        val changePercent = if (previousCount > 0) change.toDouble() / previousCount else 0.0

        when {
            changePercent < -0.8 -> {
                anomalies.add("Job count dropped by ${formatPercent(changePercent)} vs previous crawl")
                return 0.3
            }
            changePercent < -0.5 -> {
                anomalies.add("Job count dropped by ${formatPercent(changePercent)} vs previous crawl")
                return 0.6
            }
            changePercent > 2.0 -> {
                anomalies.add("Job count increased by ${formatPercent(changePercent)} vs previous crawl")
                return 0.7  // Large increase might indicate extraction issue
            }
            else -> return 1.0
        }
    }

    private fun formatPercent(value: Double): String {
        return "${(value * 100).toInt()}%"
    }

    private fun round(value: Double): Double {
        return (value * 100).toInt() / 100.0
    }
}

/**
 * Quality score result from extraction evaluation.
 */
data class QualityScore(
    val overall: Double,
    val fieldCompletionRate: Double,
    val consistencyScore: Double,
    val countScore: Double = 1.0,
    val anomalyDetected: Boolean,
    val anomalies: List<String>
) {
    /**
     * Returns quality tier for reporting.
     */
    fun getTier(): String = when {
        overall >= 0.8 -> "EXCELLENT"
        overall >= 0.6 -> "GOOD"
        overall >= 0.4 -> "NEEDS_IMPROVEMENT"
        else -> "POOR"
    }

    /**
     * Whether this extraction needs human review.
     */
    fun needsReview(): Boolean = overall < 0.5 || anomalyDetected
}
