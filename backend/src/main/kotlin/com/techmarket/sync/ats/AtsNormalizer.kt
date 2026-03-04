package com.techmarket.sync.ats

import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.sync.ats.model.NormalizedJob

/**
 * Strategy interface for normalizing provider-specific JSON data into our unified [NormalizedJob]
 * format.
 */
interface AtsNormalizer {
    /** Translates a raw JSON payload from an ATS into a list of normalized job postings. */
    fun normalize(rawData: JsonNode): List<NormalizedJob>
}
