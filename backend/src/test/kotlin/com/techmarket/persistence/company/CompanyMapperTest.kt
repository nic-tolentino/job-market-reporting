package com.techmarket.persistence.company

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.TableResult
import com.techmarket.api.model.*
import com.techmarket.models.CompanyRow
import com.techmarket.models.JobRow
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class CompanyMapperTest {

    // Helper to create a non-null FieldValue mock with string value
    private fun mockFieldValue(value: String): FieldValue {
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        every { field.stringValue } returns value
        return field
    }

    // Helper to create a non-null FieldValue mock with long value
    private fun mockFieldValue(value: Long): FieldValue {
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        every { field.longValue } returns value
        return field
    }

    // Helper to create a non-null FieldValue mock with boolean value
    private fun mockFieldValue(value: Boolean): FieldValue {
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        every { field.booleanValue } returns value
        return field
    }

    @Test
    fun `mapCompanyProfile correctly maps canonical job slug and company info`() {
        val companyId = "asb-bank"
        val detResult = mockk<TableResult>()
        val jobsResult = mockk<TableResult>()
        val aggResult = mockk<TableResult>()

        val detRow = mockk<FieldValueList>()

        // Default answer for unmocked fields - allows CompanyRow.fromCompanyRow() to hydrate
        val defaultNull = mockk<FieldValue>()
        every { defaultNull.isNull } returns true
        every { defaultNull.stringValue } returns ""
        every { defaultNull.longValue } returns 0L
        every { defaultNull.booleanValue } returns false
        every { defaultNull.repeatedValue } returns emptyList()
        every { detRow.get(any<String>()) } returns defaultNull

        // Setup Detail result fields (these override the default for specific fields)
        every { detRow.get(CompanyFields.NAME) } returns mockFieldValue("ASB Bank")

        val logoVal = mockk<FieldValue>()
        every { logoVal.isNull } returns false
        every { logoVal.stringValue } returns "http://logo.com"
        every { detRow.get(CompanyFields.LOGO_URL) } returns logoVal

        val webVal = mockk<FieldValue>()
        every { webVal.isNull } returns false
        every { webVal.stringValue } returns "http://asb.co.nz"
        every { detRow.get(CompanyFields.WEBSITE) } returns webVal

        val empVal = mockk<FieldValue>()
        every { empVal.isNull } returns false
        every { empVal.longValue } returns 5000L
        every { detRow.get(CompanyFields.EMPLOYEES_COUNT) } returns empVal

        val indVal = mockk<FieldValue>()
        every { indVal.isNull } returns false
        every { indVal.stringValue } returns "Banking"
        every { detRow.get(CompanyFields.INDUSTRIES) } returns indVal

        val descVal = mockk<FieldValue>()
        every { descVal.isNull } returns false
        every { descVal.stringValue } returns "A bank"
        every { detRow.get(CompanyFields.DESCRIPTION) } returns descVal

        // Repeated technologies/locations on company
        val compTechField = mockk<FieldValue>()
        every { compTechField.isNull } returns true
        every { detRow.get(CompanyFields.TECHNOLOGIES) } returns compTechField

        val compLocField = mockk<FieldValue>()
        every { compLocField.isNull } returns true
        every { detRow.get(CompanyFields.HIRING_LOCATIONS) } returns compLocField

        val agencyVal = mockk<FieldValue>()
        every { agencyVal.isNull } returns false
        every { agencyVal.booleanValue } returns true
        every { detRow.get(CompanyFields.IS_AGENCY) } returns agencyVal

        val socialVal = mockk<FieldValue>()
        every { socialVal.isNull } returns false
        every { socialVal.booleanValue } returns false
        every { detRow.get(CompanyFields.IS_SOCIAL_ENTERPRISE) } returns socialVal

        val hqVal = mockk<FieldValue>()
        every { hqVal.isNull } returns false
        every { hqVal.stringValue } returns "NZ"
        every { detRow.get(CompanyFields.HQ_COUNTRY) } returns hqVal

        val remoteVal = mockk<FieldValue>()
        every { remoteVal.isNull } returns false
        every { remoteVal.stringValue } returns "Remote Only"
        every { detRow.get(CompanyFields.REMOTE_POLICY) } returns remoteVal

        val visaVal = mockk<FieldValue>()
        every { visaVal.isNull } returns false
        every { visaVal.booleanValue } returns true
        every { detRow.get(CompanyFields.VISA_SPONSORSHIP) } returns visaVal

        val verifVal = mockk<FieldValue>()
        every { verifVal.isNull } returns false
        every { verifVal.stringValue } returns "verified"
        every { detRow.get(CompanyFields.VERIFICATION_LEVEL) } returns verifVal

        val opCountryItem = mockk<FieldValue>()
        every { opCountryItem.stringValue } returns "AU"
        val opCountriesField = mockk<FieldValue>()
        every { opCountriesField.isNull } returns false
        every { opCountriesField.repeatedValue } returns listOf(opCountryItem)
        every { detRow.get(CompanyFields.OPERATING_COUNTRIES) } returns opCountriesField

        val offLocItem = mockk<FieldValue>()
        every { offLocItem.stringValue } returns "Auckland"
        val offLocsField = mockk<FieldValue>()
        every { offLocsField.isNull } returns false
        every { offLocsField.repeatedValue } returns listOf(offLocItem)
        every { detRow.get(CompanyFields.OFFICE_LOCATIONS) } returns offLocsField

        every { detResult.values } returns listOf(detRow)

        // Aggregation result mock
        val aggRow = mockk<FieldValueList>()
        val topModelVal = mockk<FieldValue>()
        every { topModelVal.isNull } returns false
        every { topModelVal.stringValue } returns "Remote"
        every { aggRow.get("topModel") } returns topModelVal
        every { aggResult.values } returns listOf(aggRow)

        // Execution - hydrate typed rows and call new mapper
        val companyRow = detResult.values.firstOrNull()?.let { CompanyRow.fromCompanyRow(it) }
            ?: CompanyRow(companyId = companyId, name = "Unknown Company")
        // Construct JobRow directly instead of mocking FieldValueList
        val jobRows = listOf(
            JobRow(
                jobId = "asb-bank.nz.engineer.2023-01-01",
                jobIds = listOf("asb-bank.nz.engineer.2023-01-01"),
                applyUrls = emptyList(),
                platformLinks = emptyList(),
                locations = listOf("Auckland"),
                title = "Software Engineer",
                companyId = companyId,
                companyName = "ASB Bank",
                description = null,
                employmentType = null,
                jobFunction = null,
                salaryMin = null,
                salaryMax = null,
                postedDate = "2023-01-01",
                technologies = listOf("kotlin"),
                benefits = emptyList(),
                city = "Auckland",
                stateRegion = "Auckland",
                seniorityLevel = "Senior",
                source = "LinkedIn",
                lastSeenAt = Instant.now(),
                country = "NZ",
                workModel = null
            )
        )
        val topModel = aggResult.values.firstOrNull()?.get("topModel")?.takeIf { !it.isNull }?.stringValue
        val profile = CompanyMapper.mapCompanyProfile(companyId, companyRow, jobRows, topModel)

        // Assertions
        assertEquals("Software Engineer", profile.activeRoles[0].title)
        // CRITICAL CHECK: ensure the canonical jobId slug is mapped, NOT the platform ID
        assertEquals("asb-bank.nz.engineer.2023-01-01", profile.activeRoles[0].id)
        assertEquals("ASB Bank", profile.companyDetails.name)
        assertEquals("Remote", profile.insights.workModel)
        assertEquals(true, profile.companyDetails.isAgency)
        assertEquals("NZ", profile.companyDetails.hqCountry)
        assertEquals(listOf("AU"), profile.insights.operatingCountries)
        assertEquals(listOf("Auckland"), profile.insights.officeLocations)
    }

    @Test
    fun `mapCompanyProfile handles empty results gracefully`() {
        val emptyDet = mockk<TableResult>()
        val emptyJobs = mockk<TableResult>()
        val emptyAgg = mockk<TableResult>()

        every { emptyDet.values } returns emptyList()
        every { emptyJobs.values } returns emptyList()
        every { emptyAgg.values } returns emptyList()

        val companyRow = CompanyRow(companyId = "unknown", name = "Unknown Company")
        val jobRows = emptyList<JobRow>()
        val profile = CompanyMapper.mapCompanyProfile("unknown", companyRow, jobRows, null)

        assertEquals("Unknown Company", profile.companyDetails.name)
        assertEquals(0, profile.activeRoles.size)
    }

    @Test
    fun `mapCompanyProfile handles null optional fields in detail row`() {
        val detResult = mockk<TableResult>()
        val jobsResult = mockk<TableResult>()
        val aggResult = mockk<TableResult>()

        val detRow = mockk<FieldValueList>()

        // Default answer for unmocked fields - allows CompanyRow.fromCompanyRow() to hydrate
        val defaultNull = mockk<FieldValue>()
        every { defaultNull.isNull } returns true
        every { defaultNull.stringValue } returns ""
        every { defaultNull.longValue } returns 0L
        every { defaultNull.booleanValue } returns false
        every { defaultNull.repeatedValue } returns emptyList()
        every { detRow.get(any<String>()) } returns defaultNull

        every { detRow.get(CompanyFields.NAME) } returns mockFieldValue("Minimal Corp")

        // All optional fields are null
        val nullField = mockk<FieldValue>()
        every { nullField.isNull } returns true
        every { detRow.get(CompanyFields.ALTERNATE_NAMES) } returns nullField
        every { detRow.get(CompanyFields.LOGO_URL) } returns nullField
        every { detRow.get(CompanyFields.WEBSITE) } returns nullField
        every { detRow.get(CompanyFields.EMPLOYEES_COUNT) } returns nullField
        every { detRow.get(CompanyFields.INDUSTRIES) } returns nullField
        every { detRow.get(CompanyFields.DESCRIPTION) } returns nullField
        every { detRow.get(CompanyFields.TECHNOLOGIES) } returns nullField
        every { detRow.get(CompanyFields.HIRING_LOCATIONS) } returns nullField
        every { detRow.get(CompanyFields.IS_AGENCY) } returns nullField
        every { detRow.get(CompanyFields.IS_SOCIAL_ENTERPRISE) } returns nullField
        every { detRow.get(CompanyFields.HQ_COUNTRY) } returns nullField
        every { detRow.get(CompanyFields.OPERATING_COUNTRIES) } returns nullField
        every { detRow.get(CompanyFields.OFFICE_LOCATIONS) } returns nullField
        every { detRow.get(CompanyFields.REMOTE_POLICY) } returns nullField
        every { detRow.get(CompanyFields.VISA_SPONSORSHIP) } returns nullField
        every { detRow.get(CompanyFields.VERIFICATION_LEVEL) } returns nullField
        every { detRow.get(CompanyFields.LAST_UPDATED_AT) } returns nullField

        every { detResult.values } returns listOf(detRow)
        every { jobsResult.values } returns emptyList()
        every { aggResult.values } returns emptyList()

        val companyRow = detResult.values.firstOrNull()?.let { CompanyRow.fromCompanyRow(it) }
            ?: CompanyRow(companyId = "minimal-corp", name = "Minimal Corp")
        val jobRows = jobsResult.values.map { JobRow.fromJobRow(it) }
        val topModel = aggResult.values.firstOrNull()?.get("topModel")?.takeIf { !it.isNull }?.stringValue
        val profile = CompanyMapper.mapCompanyProfile("minimal-corp", companyRow, jobRows, topModel)

        assertEquals("Minimal Corp", profile.companyDetails.name)
        assertEquals("", profile.companyDetails.logo)
        assertEquals("", profile.companyDetails.website)
        assertEquals(0, profile.companyDetails.employeesCount)
        assertEquals(0, profile.activeRoles.size)
        assertEquals(false, profile.companyDetails.isAgency)
        assertEquals(null, profile.companyDetails.hqCountry)
    }

    @Test
    fun `mapToCompanyRecord correctly maps all curative fields from BQ row`() {
        val row = mockk<FieldValueList>()

        every { row.get(CompanyFields.COMPANY_ID) } returns mockFieldValue("comp-1")
        every { row.get(CompanyFields.NAME) } returns mockFieldValue("Comp 1")

        val altVal = mockk<FieldValue>()
        every { altVal.isNull } returns false
        every { altVal.stringValue } returns "Alt"
        val altField = mockk<FieldValue>()
        every { altField.isNull } returns false
        every { altField.repeatedValue } returns listOf(altVal)
        every { row.get(CompanyFields.ALTERNATE_NAMES) } returns altField

        val nullF = mockk<FieldValue>()
        every { nullF.isNull } returns true
        every { row.get(CompanyFields.LOGO_URL) } returns nullF
        every { row.get(CompanyFields.DESCRIPTION) } returns nullF
        every { row.get(CompanyFields.WEBSITE) } returns nullF
        every { row.get(CompanyFields.EMPLOYEES_COUNT) } returns nullF
        every { row.get(CompanyFields.INDUSTRIES) } returns nullF
        every { row.get(CompanyFields.TECHNOLOGIES) } returns nullF
        every { row.get(CompanyFields.HIRING_LOCATIONS) } returns nullF

        val boolF = mockk<FieldValue>()
        every { boolF.isNull } returns false
        every { boolF.booleanValue } returns true
        every { row.get(CompanyFields.IS_AGENCY) } returns boolF
        every { row.get(CompanyFields.IS_SOCIAL_ENTERPRISE) } returns boolF
        every { row.get(CompanyFields.VISA_SPONSORSHIP) } returns boolF

        every { row.get(CompanyFields.HQ_COUNTRY) } returns mockFieldValue("US")

        every { row.get(CompanyFields.OPERATING_COUNTRIES) } returns nullF
        every { row.get(CompanyFields.OFFICE_LOCATIONS) } returns nullF
        every { row.get(CompanyFields.REMOTE_POLICY) } returns nullF

        every { row.get(CompanyFields.VERIFICATION_LEVEL) } returns mockFieldValue("VERIFIED")

        every { row.get(CompanyFields.LAST_UPDATED_AT) } returns mockFieldValue("2023-01-01T10:00:00Z")

        val record = CompanyMapper.mapToCompanyRecord(CompanyRow.fromCompanyRow(row))

        assertEquals("comp-1", record.companyId)
        assertEquals(true, record.isAgency)
        assertEquals(true, record.isSocialEnterprise)
        assertEquals(true, record.visaSponsorship)
        assertEquals("US", record.hqCountry)
        assertEquals(listOf("Alt"), record.alternateNames)
    }

    @Test
    fun `mapCompanyProfile aggregates technologies from job postings`() {
        val companyId = "test-company"
        val detResult = mockk<TableResult>()
        val jobsResult = mockk<TableResult>()
        val aggResult = mockk<TableResult>()

        val detRow = mockk<FieldValueList>()

        // Default answer for unmocked fields - allows CompanyRow.fromCompanyRow() to hydrate
        val defaultNull = mockk<FieldValue>()
        every { defaultNull.isNull } returns true
        every { defaultNull.stringValue } returns ""
        every { defaultNull.longValue } returns 0L
        every { defaultNull.booleanValue } returns false
        every { defaultNull.repeatedValue } returns emptyList()
        every { detRow.get(any<String>()) } returns defaultNull

        every { detRow.get(CompanyFields.NAME) } returns mockFieldValue("Test Company")

        // Mock all required detail fields
        val nullField = mockk<FieldValue>()
        every { nullField.isNull } returns true

        val logoVal = mockk<FieldValue>()
        every { logoVal.isNull } returns true
        every { detRow.get(CompanyFields.LOGO_URL) } returns logoVal

        val webVal = mockk<FieldValue>()
        every { webVal.isNull } returns true
        every { detRow.get(CompanyFields.WEBSITE) } returns webVal

        val empVal = mockk<FieldValue>()
        every { empVal.isNull } returns true
        every { detRow.get(CompanyFields.EMPLOYEES_COUNT) } returns empVal

        val indVal = mockk<FieldValue>()
        every { indVal.isNull } returns true
        every { detRow.get(CompanyFields.INDUSTRIES) } returns indVal

        val descVal = mockk<FieldValue>()
        every { descVal.isNull } returns true
        every { detRow.get(CompanyFields.DESCRIPTION) } returns descVal

        // Company has no technologies (typical for manifest companies)
        every { detRow.get(CompanyFields.TECHNOLOGIES) } returns nullField

        val agencyVal = mockk<FieldValue>()
        every { agencyVal.isNull } returns true
        every { detRow.get(CompanyFields.IS_AGENCY) } returns agencyVal

        val socialVal = mockk<FieldValue>()
        every { socialVal.isNull } returns true
        every { detRow.get(CompanyFields.IS_SOCIAL_ENTERPRISE) } returns socialVal

        val hqVal = mockk<FieldValue>()
        every { hqVal.isNull } returns true
        every { detRow.get(CompanyFields.HQ_COUNTRY) } returns hqVal

        val remoteVal = mockk<FieldValue>()
        every { remoteVal.isNull } returns true
        every { detRow.get(CompanyFields.REMOTE_POLICY) } returns remoteVal

        val visaVal = mockk<FieldValue>()
        every { visaVal.isNull } returns true
        every { detRow.get(CompanyFields.VISA_SPONSORSHIP) } returns visaVal

        val verifVal = mockk<FieldValue>()
        every { verifVal.isNull } returns true
        every { detRow.get(CompanyFields.VERIFICATION_LEVEL) } returns verifVal

        every { detRow.get(CompanyFields.HIRING_LOCATIONS) } returns nullField
        every { detRow.get(CompanyFields.OPERATING_COUNTRIES) } returns nullField
        every { detRow.get(CompanyFields.OFFICE_LOCATIONS) } returns nullField

        every { detResult.values } returns listOf(detRow)

        // Helper to create a typed JobRow with given technologies
        fun createJobRow(techs: List<String>): JobRow {
            return JobRow(
                jobId = "job-1",
                jobIds = listOf("job-1"),
                applyUrls = emptyList(),
                platformLinks = emptyList(),
                locations = listOf("Auckland"),
                title = "Developer",
                companyId = companyId,
                companyName = "Test Company",
                description = null,
                employmentType = null,
                jobFunction = null,
                salaryMin = null,
                salaryMax = null,
                postedDate = "2023-01-01",
                technologies = techs,
                benefits = emptyList(),
                city = "Auckland",
                stateRegion = "Auckland",
                seniorityLevel = "Mid",
                source = "LinkedIn",
                lastSeenAt = Instant.now(),
                country = "NZ",
                workModel = null
            )
        }

        every { jobsResult.values } returns emptyList() // Not used anymore - using typed JobRows
        every { aggResult.values } returns emptyList()

        val companyRow = detResult.values.firstOrNull()?.let { CompanyRow.fromCompanyRow(it) }
            ?: CompanyRow(companyId = companyId, name = "Test Company")
        // Construct typed JobRows directly
        val jobRows = listOf(
            createJobRow(listOf("kotlin")),
            createJobRow(listOf("spring")),
            createJobRow(listOf("kotlin")) // Duplicate to test deduplication
        )
        val profile = CompanyMapper.mapCompanyProfile(companyId, companyRow, jobRows, null)

        // Should have both technologies, deduplicated and sorted
        assertEquals(listOf("Kotlin", "Spring"), profile.techStack)
    }

    @Test
    fun `mapCompanyProfile merges company and job technologies`() {
        val companyId = "test-company"
        val detResult = mockk<TableResult>()
        val jobsResult = mockk<TableResult>()
        val aggResult = mockk<TableResult>()

        val detRow = mockk<FieldValueList>()

        // Default answer for unmocked fields - allows CompanyRow.fromCompanyRow() to hydrate
        val defaultNull = mockk<FieldValue>()
        every { defaultNull.isNull } returns true
        every { defaultNull.stringValue } returns ""
        every { defaultNull.longValue } returns 0L
        every { defaultNull.booleanValue } returns false
        every { defaultNull.repeatedValue } returns emptyList()
        every { detRow.get(any<String>()) } returns defaultNull

        every { detRow.get(CompanyFields.NAME) } returns mockFieldValue("Test Company")

        // Mock all required detail fields
        val nullField = mockk<FieldValue>()
        every { nullField.isNull } returns true

        val logoVal = mockk<FieldValue>()
        every { logoVal.isNull } returns true
        every { detRow.get(CompanyFields.LOGO_URL) } returns logoVal

        val webVal = mockk<FieldValue>()
        every { webVal.isNull } returns true
        every { detRow.get(CompanyFields.WEBSITE) } returns webVal

        val empVal = mockk<FieldValue>()
        every { empVal.isNull } returns true
        every { detRow.get(CompanyFields.EMPLOYEES_COUNT) } returns empVal

        val indVal = mockk<FieldValue>()
        every { indVal.isNull } returns true
        every { detRow.get(CompanyFields.INDUSTRIES) } returns indVal

        val descVal = mockk<FieldValue>()
        every { descVal.isNull } returns true
        every { detRow.get(CompanyFields.DESCRIPTION) } returns descVal

        // Company has manually curated technologies
        val compTechField = mockk<FieldValue>()
        every { compTechField.isNull } returns false
        every { compTechField.repeatedValue } returns listOf(mockFieldValue("aws"))
        every { detRow.get(CompanyFields.TECHNOLOGIES) } returns compTechField

        val agencyVal = mockk<FieldValue>()
        every { agencyVal.isNull } returns true
        every { detRow.get(CompanyFields.IS_AGENCY) } returns agencyVal

        val socialVal = mockk<FieldValue>()
        every { socialVal.isNull } returns true
        every { detRow.get(CompanyFields.IS_SOCIAL_ENTERPRISE) } returns socialVal

        val hqVal = mockk<FieldValue>()
        every { hqVal.isNull } returns true
        every { detRow.get(CompanyFields.HQ_COUNTRY) } returns hqVal

        val remoteVal = mockk<FieldValue>()
        every { remoteVal.isNull } returns true
        every { detRow.get(CompanyFields.REMOTE_POLICY) } returns remoteVal

        val visaVal = mockk<FieldValue>()
        every { visaVal.isNull } returns true
        every { detRow.get(CompanyFields.VISA_SPONSORSHIP) } returns visaVal

        val verifVal = mockk<FieldValue>()
        every { verifVal.isNull } returns true
        every { detRow.get(CompanyFields.VERIFICATION_LEVEL) } returns verifVal

        every { detRow.get(CompanyFields.HIRING_LOCATIONS) } returns nullField
        every { detRow.get(CompanyFields.OPERATING_COUNTRIES) } returns nullField
        every { detRow.get(CompanyFields.OFFICE_LOCATIONS) } returns nullField

        every { detResult.values } returns listOf(detRow)

        // Helper to create a typed JobRow with given technologies
        fun createJobRow(techs: List<String>): JobRow {
            return JobRow(
                jobId = "job-1",
                jobIds = listOf("job-1"),
                applyUrls = emptyList(),
                platformLinks = emptyList(),
                locations = listOf("Auckland"),
                title = "Developer",
                companyId = companyId,
                companyName = "Test Company",
                description = null,
                employmentType = null,
                jobFunction = null,
                salaryMin = null,
                salaryMax = null,
                postedDate = "2023-01-01",
                technologies = techs,
                benefits = emptyList(),
                city = "Auckland",
                stateRegion = "Auckland",
                seniorityLevel = "Mid",
                source = "LinkedIn",
                lastSeenAt = Instant.now(),
                country = "NZ",
                workModel = null
            )
        }

        every { jobsResult.values } returns emptyList()
        every { aggResult.values } returns emptyList()

        val companyRow = detResult.values.firstOrNull()?.let { CompanyRow.fromCompanyRow(it) }
            ?: CompanyRow(companyId = companyId, name = "Test Company")
        // Construct typed JobRows directly
        val jobRows = listOf(createJobRow(listOf("kotlin")))
        val profile = CompanyMapper.mapCompanyProfile(companyId, companyRow, jobRows, null)

        // Should merge both sources, deduplicated and sorted
        assertEquals(listOf("AWS", "Kotlin"), profile.techStack)
    }

    @Test
    fun `mapCompanyProfile handles jobs with no technologies`() {
        val companyId = "test-company"
        val detResult = mockk<TableResult>()
        val jobsResult = mockk<TableResult>()
        val aggResult = mockk<TableResult>()

        val detRow = mockk<FieldValueList>()

        // Default answer for unmocked fields - allows CompanyRow.fromCompanyRow() to hydrate
        val defaultNull = mockk<FieldValue>()
        every { defaultNull.isNull } returns true
        every { defaultNull.stringValue } returns ""
        every { defaultNull.longValue } returns 0L
        every { defaultNull.booleanValue } returns false
        every { defaultNull.repeatedValue } returns emptyList()
        every { detRow.get(any<String>()) } returns defaultNull

        every { detRow.get(CompanyFields.NAME) } returns mockFieldValue("Test Company")

        val nullField = mockk<FieldValue>()
        every { nullField.isNull } returns true

        val logoVal = mockk<FieldValue>()
        every { logoVal.isNull } returns true
        every { detRow.get(CompanyFields.LOGO_URL) } returns logoVal

        val webVal = mockk<FieldValue>()
        every { webVal.isNull } returns true
        every { detRow.get(CompanyFields.WEBSITE) } returns webVal

        val empVal = mockk<FieldValue>()
        every { empVal.isNull } returns true
        every { detRow.get(CompanyFields.EMPLOYEES_COUNT) } returns empVal

        val indVal = mockk<FieldValue>()
        every { indVal.isNull } returns true
        every { detRow.get(CompanyFields.INDUSTRIES) } returns indVal

        val descVal = mockk<FieldValue>()
        every { descVal.isNull } returns true
        every { detRow.get(CompanyFields.DESCRIPTION) } returns descVal

        every { detRow.get(CompanyFields.TECHNOLOGIES) } returns nullField

        val agencyVal = mockk<FieldValue>()
        every { agencyVal.isNull } returns true
        every { detRow.get(CompanyFields.IS_AGENCY) } returns agencyVal

        val socialVal = mockk<FieldValue>()
        every { socialVal.isNull } returns true
        every { detRow.get(CompanyFields.IS_SOCIAL_ENTERPRISE) } returns socialVal

        val hqVal = mockk<FieldValue>()
        every { hqVal.isNull } returns true
        every { detRow.get(CompanyFields.HQ_COUNTRY) } returns hqVal

        val remoteVal = mockk<FieldValue>()
        every { remoteVal.isNull } returns true
        every { detRow.get(CompanyFields.REMOTE_POLICY) } returns remoteVal

        val visaVal = mockk<FieldValue>()
        every { visaVal.isNull } returns true
        every { detRow.get(CompanyFields.VISA_SPONSORSHIP) } returns visaVal

        val verifVal = mockk<FieldValue>()
        every { verifVal.isNull } returns true
        every { detRow.get(CompanyFields.VERIFICATION_LEVEL) } returns verifVal

        every { detRow.get(CompanyFields.HIRING_LOCATIONS) } returns nullField
        every { detRow.get(CompanyFields.OPERATING_COUNTRIES) } returns nullField
        every { detRow.get(CompanyFields.OFFICE_LOCATIONS) } returns nullField

        every { detResult.values } returns listOf(detRow)

        every { jobsResult.values } returns emptyList()
        every { aggResult.values } returns emptyList()

        val companyRow = detResult.values.firstOrNull()?.let { CompanyRow.fromCompanyRow(it) }
            ?: CompanyRow(companyId = companyId, name = "Test Company")
        // Construct typed JobRow with no technologies
        val jobRows = listOf(
            JobRow(
                jobId = "job-1",
                jobIds = listOf("job-1"),
                applyUrls = emptyList(),
                platformLinks = emptyList(),
                locations = listOf("Auckland"),
                title = "Developer",
                companyId = companyId,
                companyName = "Test Company",
                description = null,
                employmentType = null,
                jobFunction = null,
                salaryMin = null,
                salaryMax = null,
                postedDate = "2023-01-01",
                technologies = emptyList(),
                benefits = emptyList(),
                city = "Auckland",
                stateRegion = "Auckland",
                seniorityLevel = "Mid",
                source = "LinkedIn",
                lastSeenAt = Instant.now(),
                country = "NZ",
                workModel = null
            )
        )
        val profile = CompanyMapper.mapCompanyProfile(companyId, companyRow, jobRows, null)

        // Should return empty list when no technologies anywhere
        assertEquals(emptyList<String>(), profile.techStack)
    }
}
