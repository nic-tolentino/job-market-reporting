package com.techmarket.models

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.techmarket.persistence.CompanyAliases
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Null-safety regression tests for QueryRows hydration methods.
 * 
 * These tests ensure that all .from*Row() methods handle null fields gracefully
 * and never throw NullPointerException at runtime. This is the key test suite
 * that prevents the original class of bug from recurring.
 */
class QueryRowsTest {

    @Test
    fun `JobRow fromJobRow should handle all null fields without throwing`() {
        val fieldList = createAllNullFieldList()

        val jobRow = JobRow.fromJobRow(fieldList)

        // Should not throw and should return sensible defaults
        assertNotNull(jobRow)
        assertEquals("test-job-id", jobRow.jobId)
        assertEquals("Unknown Title", jobRow.title)
        assertEquals("unknown", jobRow.companyId)
        assertEquals("Unknown Company", jobRow.companyName)
        assertEquals("Mid-Level", jobRow.seniorityLevel)
        assertEquals("Unknown", jobRow.source)
        assertEquals("", jobRow.postedDate)
        assertEquals("Unknown", jobRow.city)
        assertEquals("Unknown", jobRow.stateRegion)
        assertTrue(jobRow.jobIds.isEmpty())
        assertTrue(jobRow.applyUrls.isEmpty())
        assertTrue(jobRow.platformLinks.isEmpty())
        assertTrue(jobRow.locations.isEmpty())
        assertTrue(jobRow.technologies.isEmpty())
        assertTrue(jobRow.benefits.isEmpty())
    }

    @Test
    fun `JobRow fromJobRow should handle partial null fields`() {
        val fieldList = createPartialNullFieldList()

        val jobRow = JobRow.fromJobRow(fieldList)

        assertNotNull(jobRow)
        assertEquals("job-123", jobRow.jobId)
        assertEquals("Software Engineer", jobRow.title)
        assertEquals("company-456", jobRow.companyId)
        assertEquals("Tech Corp", jobRow.companyName)
        assertEquals("Senior", jobRow.seniorityLevel)
        assertEquals("LinkedIn", jobRow.source)
        assertEquals("2026-03-05", jobRow.postedDate)
        // Null fields should return null (not defaults) for optional fields
        assertEquals(null, jobRow.description)
        assertEquals(null, jobRow.employmentType)
        assertEquals(null, jobRow.jobFunction)
        assertEquals(null, jobRow.country)
        assertEquals(null, jobRow.workModel)
    }

    @Test
    fun `CompanyInfoRow fromJoinedRow should handle all null fields without throwing`() {
        val fieldList = createAllNullCompanyFieldList()

        val companyRow = CompanyInfoRow.fromJoinedRow(fieldList)

        // Should not throw and should return sensible defaults
        assertNotNull(companyRow)
        assertEquals("unknown", companyRow.companyId)
        assertEquals("Unknown Company", companyRow.name)
        assertEquals("", companyRow.logoUrl)
        assertEquals("", companyRow.description)
        assertEquals("", companyRow.website)
        assertEquals("VERIFIED", companyRow.verificationLevel)
        assertTrue(companyRow.hiringLocations.isEmpty())
    }

    @Test
    fun `CompanyInfoRow fromJoinedRow should handle partial null fields`() {
        val fieldList = createPartialCompanyFieldList()

        val companyRow = CompanyInfoRow.fromJoinedRow(fieldList)

        assertNotNull(companyRow)
        assertEquals("comp-789", companyRow.companyId)
        assertEquals("Innovate Inc", companyRow.name)
        assertEquals("https://example.com/logo.png", companyRow.logoUrl)
        assertEquals("We build cool stuff", companyRow.description)
        assertEquals("https://example.com", companyRow.website)
        assertEquals("unverified", companyRow.verificationLevel)
        assertEquals(null, companyRow.hqCountry)
        assertTrue(companyRow.hiringLocations.isNotEmpty())
    }

    @Test
    fun `SeniorityRow fromAggregationRow should handle null fields`() {
        val fieldList = createAllNullSeniorityFieldList()

        val seniorityRow = SeniorityRow.fromAggregationRow(fieldList)

        assertNotNull(seniorityRow)
        assertEquals("Unknown", seniorityRow.name)
        assertEquals(0, seniorityRow.value)
    }

    @Test
    fun `SeniorityRow fromAggregationRow should handle valid data`() {
        val fieldList = createSeniorityFieldList("Mid-Level", 42)

        val seniorityRow = SeniorityRow.fromAggregationRow(fieldList)

        assertNotNull(seniorityRow)
        assertEquals("Mid-Level", seniorityRow.name)
        assertEquals(42, seniorityRow.value)
    }

    @Test
    fun `CompanyLeaderboardRow fromAggregationRow should handle null fields`() {
        val fieldList = createAllNullCompanyLeaderboardFieldList()

        val leaderboardRow = CompanyLeaderboardRow.fromAggregationRow(fieldList)

        assertNotNull(leaderboardRow)
        assertEquals("unknown", leaderboardRow.id)
        assertEquals("Unknown Company", leaderboardRow.name)
        assertEquals("", leaderboardRow.logo)
        assertEquals(0, leaderboardRow.activeRoles)
    }

    @Test
    fun `CompanyLeaderboardRow fromAggregationRow should handle valid data`() {
        val fieldList = createCompanyLeaderboardFieldList(
            id = "google",
            name = "Google",
            logo = "https://logo.png",
            activeRoles = 15
        )

        val leaderboardRow = CompanyLeaderboardRow.fromAggregationRow(fieldList)

        assertNotNull(leaderboardRow)
        assertEquals("google", leaderboardRow.id)
        assertEquals("Google", leaderboardRow.name)
        assertEquals("https://logo.png", leaderboardRow.logo)
        assertEquals(15, leaderboardRow.activeRoles)
    }

    @Test
    fun `JobDetailsRow fromJoinedRow should delegate to JobRow and CompanyInfoRow`() {
        val fieldList = createCompleteFieldList()

        val detailsRow = JobDetailsRow.fromJoinedRow(fieldList)

        assertNotNull(detailsRow)
        assertNotNull(detailsRow.job)
        assertNotNull(detailsRow.company)
        assertEquals("job-123", detailsRow.job.jobId)
        assertEquals("comp-456", detailsRow.company.companyId)
    }

    @Test
    fun `CompanyRow fromCompanyRow should handle all null fields without throwing`() {
        val fieldList = createAllNullCompanyRowFieldList()

        val companyRow = CompanyRow.fromCompanyRow(fieldList)

        // Should not throw and should return sensible defaults
        assertNotNull(companyRow)
        assertEquals("unknown", companyRow.companyId)
        assertEquals("Unknown Company", companyRow.name)
        assertTrue(companyRow.alternateNames.isEmpty())
        assertEquals(null, companyRow.logoUrl)
        assertEquals(null, companyRow.website)
        assertEquals(null, companyRow.employeesCount)
        assertEquals(null, companyRow.industries)
        assertEquals(null, companyRow.description)
        assertTrue(companyRow.technologies.isEmpty())
        assertTrue(companyRow.hiringLocations.isEmpty())
        assertEquals(false, companyRow.isAgency)
        assertEquals(false, companyRow.isSocialEnterprise)
        assertEquals(null, companyRow.hqCountry)
        assertTrue(companyRow.operatingCountries.isEmpty())
        assertTrue(companyRow.officeLocations.isEmpty())
        assertEquals(null, companyRow.remotePolicy)
        assertEquals(false, companyRow.visaSponsorship)
        assertEquals("VERIFIED", companyRow.verificationLevel)
    }

    // Helper methods to create mock FieldValueList with various null configurations

    private fun createAllNullFieldList(): FieldValueList {
        val fields = listOf(
            JobFields.JOB_ID, JobFields.JOB_IDS, JobFields.APPLY_URLS,
            JobFields.PLATFORM_LINKS, JobFields.LOCATIONS, JobFields.TITLE,
            JobFields.COMPANY_ID, JobFields.COMPANY_NAME, JobFields.DESCRIPTION,
            JobFields.EMPLOYMENT_TYPE, JobFields.JOB_FUNCTION, JobFields.SALARY_MIN,
            JobFields.SALARY_MAX, JobFields.POSTED_DATE, JobFields.TECHNOLOGIES,
            JobFields.BENEFITS, JobFields.CITY, JobFields.STATE_REGION,
            JobFields.SENIORITY_LEVEL, JobFields.SOURCE, JobFields.LAST_SEEN_AT,
            JobFields.COUNTRY, JobFields.WORK_MODEL
        )
        val fieldList = createMockFieldList(fields, allNull = true); setupField(fieldList, JobFields.JOB_ID, "test-job-id"); return fieldList
    }

    private fun createPartialNullFieldList(): FieldValueList {
        val fieldList = mockk<FieldValueList>()
        
        // Set up default answer for any field access - return a null FieldValue
        every { fieldList.get(any<String>()) } answers {
            val field = mockk<FieldValue>()
            every { field.isNull } returns true
            field
        }
        
        // Set up required fields with values
        setupField(fieldList, JobFields.JOB_ID, "job-123")
        setupField(fieldList, JobFields.TITLE, "Software Engineer")
        setupField(fieldList, JobFields.COMPANY_ID, "company-456")
        setupField(fieldList, JobFields.COMPANY_NAME, "Tech Corp")
        setupField(fieldList, JobFields.SENIORITY_LEVEL, "Senior")
        setupField(fieldList, JobFields.SOURCE, "LinkedIn")
        setupField(fieldList, JobFields.POSTED_DATE, "2026-03-05")
        setupField(fieldList, JobFields.CITY, "San Francisco")
        setupField(fieldList, JobFields.STATE_REGION, "CA")
        setupField(fieldList, JobFields.LAST_SEEN_AT, "2026-03-05T12:00:00Z")
        
        // Set up list fields
        setupListField(fieldList, JobFields.JOB_IDS, listOf("job-123"))
        setupListField(fieldList, JobFields.LOCATIONS, listOf("San Francisco, CA"))
        setupListField(fieldList, JobFields.TECHNOLOGIES, listOf("kotlin"))
        setupListField(fieldList, JobFields.BENEFITS, listOf("Health"))
        
        // Set up empty list fields (not null)
        setupListField(fieldList, JobFields.APPLY_URLS, emptyList())
        setupListField(fieldList, JobFields.PLATFORM_LINKS, emptyList())
        
        return fieldList
    }

    private fun createCompleteFieldList(): FieldValueList {
        val fieldList = mockk<FieldValueList>()
        
        // Set up default answer for any field access - return a null FieldValue
        every { fieldList.get(any<String>()) } answers {
            val field = mockk<FieldValue>()
            every { field.isNull } returns true
            field
        }

        // Job fields
        setupField(fieldList, JobFields.JOB_ID, "job-123")
        setupField(fieldList, JobFields.TITLE, "Software Engineer")
        setupField(fieldList, JobFields.COMPANY_ID, "comp-456")
        setupField(fieldList, JobFields.COMPANY_NAME, "Tech Corp")
        setupField(fieldList, JobFields.SENIORITY_LEVEL, "Senior")
        setupField(fieldList, JobFields.SOURCE, "LinkedIn")
        setupField(fieldList, JobFields.POSTED_DATE, "2026-03-05")
        setupField(fieldList, JobFields.CITY, "San Francisco")
        setupField(fieldList, JobFields.STATE_REGION, "CA")
        setupField(fieldList, JobFields.LAST_SEEN_AT, "2026-03-05T12:00:00Z")
        setupField(fieldList, JobFields.DESCRIPTION, "Build cool stuff")
        setupField(fieldList, JobFields.EMPLOYMENT_TYPE, "Full-time")
        setupField(fieldList, JobFields.JOB_FUNCTION, "Engineering")
        setupField(fieldList, JobFields.COUNTRY, "US")
        setupField(fieldList, JobFields.WORK_MODEL, "Hybrid")
        
        // List fields
        setupListField(fieldList, JobFields.JOB_IDS, listOf("job-123"))
        setupListField(fieldList, JobFields.LOCATIONS, listOf("San Francisco, CA"))
        setupListField(fieldList, JobFields.TECHNOLOGIES, listOf("kotlin"))
        setupListField(fieldList, JobFields.BENEFITS, listOf("Health"))
        setupListField(fieldList, JobFields.APPLY_URLS, listOf("https://apply.com"))
        setupListField(fieldList, JobFields.PLATFORM_LINKS, listOf("https://link.com"))
        
        // Company fields (aliased)
        setupField(fieldList, CompanyAliases.COMPANY_ID, "comp-456")
        setupField(fieldList, CompanyAliases.NAME, "Tech Corp")
        setupField(fieldList, CompanyAliases.LOGO_URL, "https://logo.png")
        setupField(fieldList, CompanyAliases.DESCRIPTION, "We build cool stuff")
        setupField(fieldList, CompanyAliases.WEBSITE, "https://techcorp.com")
        setupListField(fieldList, CompanyAliases.HIRING_LOCATIONS, listOf("San Francisco", "Remote"))
        setupField(fieldList, CompanyAliases.HQ_COUNTRY, "US")
        setupField(fieldList, CompanyAliases.VERIFICATION_LEVEL, "VERIFIED")
        
        return fieldList
    }

    private fun createAllNullCompanyFieldList(): FieldValueList {
        val fields = listOf(
            CompanyAliases.COMPANY_ID, CompanyAliases.NAME, CompanyAliases.LOGO_URL,
            CompanyAliases.DESCRIPTION, CompanyAliases.WEBSITE, CompanyAliases.HIRING_LOCATIONS,
            CompanyAliases.HQ_COUNTRY, CompanyAliases.VERIFICATION_LEVEL
        )
        val fieldList = createMockFieldList(fields, allNull = true); setupField(fieldList, JobFields.JOB_ID, "test-job-id"); return fieldList
    }

    private fun createAllNullCompanyRowFieldList(): FieldValueList {
        val fields = listOf(
            CompanyFields.COMPANY_ID, CompanyFields.NAME, CompanyFields.ALTERNATE_NAMES,
            CompanyFields.LOGO_URL, CompanyFields.WEBSITE, CompanyFields.EMPLOYEES_COUNT,
            CompanyFields.INDUSTRIES, CompanyFields.DESCRIPTION, CompanyFields.TECHNOLOGIES,
            CompanyFields.HIRING_LOCATIONS, CompanyFields.IS_AGENCY, CompanyFields.IS_SOCIAL_ENTERPRISE,
            CompanyFields.HQ_COUNTRY, CompanyFields.OPERATING_COUNTRIES, CompanyFields.OFFICE_LOCATIONS,
            CompanyFields.REMOTE_POLICY, CompanyFields.VISA_SPONSORSHIP, CompanyFields.VISA_SPONSORSHIP_DETAIL, CompanyFields.VERIFICATION_LEVEL,
            CompanyFields.LAST_UPDATED_AT
        )
        val fieldList = createMockFieldList(fields, allNull = true); setupField(fieldList, JobFields.JOB_ID, "test-job-id"); return fieldList
    }

    private fun createPartialCompanyFieldList(): FieldValueList {
        val fieldList = mockk<FieldValueList>()
        
        // Set up default answer for any field access - return a null FieldValue
        every { fieldList.get(any<String>()) } answers {
            val field = mockk<FieldValue>()
            every { field.isNull } returns true
            field
        }

        setupField(fieldList, CompanyAliases.COMPANY_ID, "comp-789")
        setupField(fieldList, CompanyAliases.NAME, "Innovate Inc")
        setupField(fieldList, CompanyAliases.LOGO_URL, "https://example.com/logo.png")
        setupField(fieldList, CompanyAliases.DESCRIPTION, "We build cool stuff")
        setupField(fieldList, CompanyAliases.WEBSITE, "https://example.com")
        setupField(fieldList, CompanyAliases.VERIFICATION_LEVEL, "unverified")
        
        setupListField(fieldList, CompanyAliases.HIRING_LOCATIONS, listOf("Remote", "NYC"))
        // HQ_COUNTRY is null by default (not set up)
        
        return fieldList
    }

    private fun createAllNullSeniorityFieldList(): FieldValueList {
        return createMockFieldList(listOf("name", "value"), allNull = true)
    }

    private fun createSeniorityFieldList(name: String, value: Int): FieldValueList {
        val fieldList = mockk<FieldValueList>()
        setupField(fieldList, "name", name)
        setupField(fieldList, "value", value.toString())
        return fieldList
    }

    private fun createAllNullCompanyLeaderboardFieldList(): FieldValueList {
        return createMockFieldList(listOf("id", "name", "logo", "activeRoles"), allNull = true)
    }

    private fun createCompanyLeaderboardFieldList(
        id: String,
        name: String,
        logo: String,
        activeRoles: Int
    ): FieldValueList {
        val fieldList = mockk<FieldValueList>()
        setupField(fieldList, "id", id)
        setupField(fieldList, "name", name)
        setupField(fieldList, "logo", logo)
        setupField(fieldList, "activeRoles", activeRoles.toString())
        return fieldList
    }

    private fun createMockFieldList(fields: List<String>, allNull: Boolean): FieldValueList {
        val fieldList = mockk<FieldValueList>()
        fields.forEach { fieldName ->
            val field = mockk<FieldValue>()
            every { field.isNull } returns allNull
            if (!allNull) {
                every { field.stringValue } returns ""
                every { field.longValue } returns 0L
            }
            every { fieldList.get(fieldName) } returns field
        }
        return fieldList
    }

    private fun setupField(fieldList: FieldValueList, fieldName: String, value: String) {
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        every { field.stringValue } returns value
        every { field.longValue } returns (value.toLongOrNull() ?: 0L)
        every { fieldList.get(fieldName) } returns field
    }

    private fun setupListField(fieldList: FieldValueList, fieldName: String, values: List<String>) {
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        val repeatedValues = values.map { value ->
            val repeatedField = mockk<FieldValue>()
            every { repeatedField.isNull } returns false
            every { repeatedField.stringValue } returns value
            repeatedField
        }
        every { field.repeatedValue } returns repeatedValues
        every { fieldList.get(fieldName) } returns field
    }
}
