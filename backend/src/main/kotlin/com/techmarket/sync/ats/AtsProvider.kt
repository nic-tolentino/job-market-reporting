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
    WORKDAY("Workday"),
    BAMBOOHR("BambooHR"),
    TEAMTAILOR("Teamtailor"),
    SMARTRECRUITERS("SmartRecruiters"),
    WORKABLE("Workable"),
    SUCCESSFACTORS("SuccessFactors"),
    APIFY("LinkedIn-Apify"); // Aligns with how it appears in sources

    companion object {
        val ATS_SOURCES =
                listOf(
                                GREENHOUSE,
                                LEVER,
                                ASHBY,
                                JOBADDER,
                                EMPLOYMENT_HERO,
                                SNAPHIRE,
                                WORKDAY,
                                BAMBOOHR,
                                TEAMTAILOR,
                                SMARTRECRUITERS,
                                WORKABLE,
                                SUCCESSFACTORS
                        )
                        .map { it.displayName }
    }
}
