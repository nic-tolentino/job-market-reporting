package com.techmarket.sync.ats

/**
 * Supported Applicant Tracking System (ATS) providers. APIFY is included as the
 * legacy/supplementary scraper source.
 */
enum class AtsProvider(val displayName: String) {
    GREENHOUSE("Greenhouse"),
    LEVER("Lever"),
    ASHBY("Ashby"),
    JOBADDER("JobAdder"),
    EMPLOYMENT_HERO("Employment Hero"),
    SNAPHIRE("SnapHire"),
    APIFY("LinkedIn-Apify"); // Aligns with how it appears in sources

    companion object {
        val ATS_SOURCES =
                listOf(GREENHOUSE, LEVER, ASHBY, JOBADDER, EMPLOYMENT_HERO, SNAPHIRE).map {
                    it.displayName
                }
    }
}
