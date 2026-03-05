package com.techmarket.persistence.job

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.techmarket.persistence.JobFields
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class JobMapperTest {

    @Test
    fun `mapToJobRecord should handle ISO date strings`() {
        val row = mock(FieldValueList::class.java)
        
        // Mocking behavior for a successful ISO date parse
        setupMockRow(row, mapOf(
            JobFields.JOB_ID to "test-job-id",
            JobFields.COMPANY_ID to "test-company-id",
            JobFields.COMPANY_NAME to "Test Company",
            JobFields.SOURCE to "LinkedIn",
            JobFields.COUNTRY to "NZ",
            JobFields.CITY to "Auckland",
            JobFields.STATE_REGION to "Auckland",
            JobFields.TITLE to "Software Engineer",
            JobFields.SENIORITY_LEVEL to "Senior",
            JobFields.POSTED_DATE to "2026-03-05", // ISO Format
            JobFields.LAST_SEEN_AT to "2026-03-05T18:00:00Z"
        ))

        val record = JobMapper.mapToJobRecord(row)
        
        assertEquals(LocalDate.of(2026, 3, 5), record.postedDate)
        assertEquals("test-job-id", record.jobId)
    }

    @Test
    fun `mapToJobRecord should handle numeric scientific timestamp strings`() {
        val row = mock(FieldValueList::class.java)
        
        // This simulates what BigQuery returns: a scientific notation string for an epoch timestamp
        val scientificTimestamp = "1.772441037962E9"
        val expectedDate = Instant.ofEpochSecond(1772441037L)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()

        setupMockRow(row, mapOf(
            JobFields.POSTED_DATE to scientificTimestamp,
            JobFields.LAST_SEEN_AT to scientificTimestamp
        ))

        val record = JobMapper.mapToJobRecord(row)
        
        assertEquals(expectedDate, record.postedDate)
        assertEquals(Instant.ofEpochSecond(1772441037L), record.lastSeenAt)
    }

    @Test
    fun `mapToJobRecord should handle null dates gracefully`() {
        val row = mock(FieldValueList::class.java)
        
        val field = mock(FieldValue::class.java)
        `when`(field.isNull).thenReturn(true)
        `when`(row.get(JobFields.POSTED_DATE)).thenReturn(field)
        
        // Setup other required fields as non-null dummy values
        setupMockRow(row, mapOf(
            JobFields.JOB_ID to "id",
            JobFields.COMPANY_ID to "comp",
            JobFields.COMPANY_NAME to "Name",
            JobFields.SOURCE to "Src",
            JobFields.COUNTRY to "NZ",
            JobFields.TITLE to "Title",
            JobFields.SENIORITY_LEVEL to "Mid",
            JobFields.CITY to "City",
            JobFields.STATE_REGION to "Reg",
            JobFields.LAST_SEEN_AT to "2026-01-01T00:00:00Z"
        ), skipFields = setOf(JobFields.POSTED_DATE))

        val record = JobMapper.mapToJobRecord(row)
        assertNull(record.postedDate)
    }

    private fun setupMockRow(row: FieldValueList, values: Map<String, String>, skipFields: Set<String> = emptySet()) {
        // Defaults for required fields if not provided
        val defaults = mapOf(
            JobFields.JOB_ID to "default-id",
            JobFields.COMPANY_ID to "default-company",
            JobFields.COMPANY_NAME to "Default Co",
            JobFields.SOURCE to "Manual",
            JobFields.COUNTRY to "NZ",
            JobFields.TITLE to "Dev",
            JobFields.SENIORITY_LEVEL to "Junior",
            JobFields.CITY to "Auckland",
            JobFields.STATE_REGION to "",
            JobFields.LAST_SEEN_AT to Instant.now().toString()
        )

        val finalValues = defaults + values

        // List of all fields the mapper touches
        val allFields = listOf(
            JobFields.JOB_ID, JobFields.PLATFORM_JOB_IDS, JobFields.APPLY_URLS, 
            JobFields.PLATFORM_LINKS, JobFields.LOCATIONS, JobFields.COMPANY_ID,
            JobFields.COMPANY_NAME, JobFields.SOURCE, JobFields.COUNTRY, JobFields.CITY,
            JobFields.STATE_REGION, JobFields.TITLE, JobFields.SENIORITY_LEVEL,
            JobFields.TECHNOLOGIES, JobFields.SALARY_MIN, JobFields.SALARY_MAX,
            JobFields.POSTED_DATE, JobFields.BENEFITS, JobFields.EMPLOYMENT_TYPE,
            JobFields.WORK_MODEL, JobFields.JOB_FUNCTION, JobFields.DESCRIPTION, 
            JobFields.LAST_SEEN_AT
        )

        allFields.forEach { fieldName ->
            if (skipFields.contains(fieldName)) return@forEach
            
            val field = mock(FieldValue::class.java)
            val value = finalValues[fieldName]
            
            if (value == null) {
                `when`(field.isNull).thenReturn(true)
            } else {
                `when`(field.isNull).thenReturn(false)
                `when`(field.stringValue).thenReturn(value)
                // Handle cases where longValue is called (salary)
                if (fieldName == JobFields.SALARY_MIN || fieldName == JobFields.SALARY_MAX) {
                    `when`(field.longValue).thenReturn(value.toLong())
                }
            }
            `when`(row.get(fieldName)).thenReturn(field)
        }
    }
}
