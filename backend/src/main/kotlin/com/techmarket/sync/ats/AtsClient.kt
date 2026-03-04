package com.techmarket.sync.ats

/** Interface for communicating with external ATS APIs. */
interface AtsClient {
    /**
     * Fetches the complete set of job postings for a given company identifier. Returns the raw JSON
     * response as a string to be stored in the Bronze layer.
     */
    fun fetchJobs(identifier: String): String
}
