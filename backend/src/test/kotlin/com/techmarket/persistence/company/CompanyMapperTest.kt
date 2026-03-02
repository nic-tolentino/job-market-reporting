package com.techmarket.persistence.company

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.TableResult
import com.techmarket.api.model.*
import com.techmarket.persistence.CompanyFields
import com.techmarket.persistence.JobFields
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CompanyMapperTest {

    @Test
    fun `mapCompanyProfile correctly maps canonical job slug and company info`() {
        val companyId = "asb-bank"
        val detResult = mockk<TableResult>()
        val jobsResult = mockk<TableResult>()
        val aggResult = mockk<TableResult>()

        val detRow = mockk<FieldValueList>()

        // Setup Detail result fields
        val nameVal = mockk<FieldValue>()
        every { nameVal.stringValue } returns "ASB Bank"
        every { detRow.get(CompanyFields.NAME) } returns nameVal

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

        every { detResult.values } returns listOf(detRow)

        // Setup Jobs result fields
        val jobRow = mockk<FieldValueList>()

        val canonIdVal = mockk<FieldValue>()
        every { canonIdVal.stringValue } returns "asb-bank.nz.engineer.2023-01-01"
        every { jobRow.get(JobFields.JOB_ID) } returns canonIdVal

        val titleVal = mockk<FieldValue>()
        every { titleVal.stringValue } returns "Software Engineer"
        every { jobRow.get(JobFields.TITLE) } returns titleVal

        val cityVal = mockk<FieldValue>()
        every { cityVal.isNull } returns false
        every { cityVal.stringValue } returns "Auckland"
        every { jobRow.get(JobFields.CITY) } returns cityVal

        val stateVal = mockk<FieldValue>()
        every { stateVal.isNull } returns false
        every { stateVal.stringValue } returns "Auckland"
        every { jobRow.get(JobFields.STATE_REGION) } returns stateVal

        val techItemVal = mockk<FieldValue>()
        every { techItemVal.stringValue } returns "kotlin"
        val techField = mockk<FieldValue>()
        every { techField.isNull } returns false
        every { techField.repeatedValue } returns listOf(techItemVal)
        every { jobRow.get(JobFields.TECHNOLOGIES) } returns techField

        val platIdVal = mockk<FieldValue>()
        every { platIdVal.stringValue } returns "4379750292"
        val platIdsField = mockk<FieldValue>()
        every { platIdsField.isNull } returns false
        every { platIdsField.repeatedValue } returns listOf(platIdVal)
        every { jobRow.get(JobFields.JOB_IDS) } returns platIdsField

        val nullField = mockk<FieldValue>()
        every { nullField.isNull } returns true
        every { jobRow.get(JobFields.APPLY_URLS) } returns nullField
        every { jobRow.get(JobFields.PLATFORM_LINKS) } returns nullField
        every { jobRow.get(JobFields.SALARY_MIN) } returns nullField
        every { jobRow.get(JobFields.SALARY_MAX) } returns nullField
        every { jobRow.get(JobFields.BENEFITS) } returns nullField

        val dateVal = mockk<FieldValue>()
        every { dateVal.isNull } returns false
        every { dateVal.stringValue } returns "2023-01-01"
        every { jobRow.get(JobFields.POSTED_DATE) } returns dateVal

        val seniorVal = mockk<FieldValue>()
        every { seniorVal.isNull } returns false
        every { seniorVal.stringValue } returns "Senior"
        every { jobRow.get(JobFields.SENIORITY_LEVEL) } returns seniorVal

        every { jobsResult.values } returns listOf(jobRow)

        // Aggregation result mock
        val aggRow = mockk<FieldValueList>()
        val topModelVal = mockk<FieldValue>()
        every { topModelVal.isNull } returns false
        every { topModelVal.stringValue } returns "Remote"
        every { aggRow.get("topModel") } returns topModelVal
        every { aggResult.values } returns listOf(aggRow)

        // Execution
        val profile = CompanyMapper.mapCompanyProfile(companyId, detResult, jobsResult, aggResult)

        // Assertions
        assertEquals("Software Engineer", profile.activeRoles[0].title)
        // CRITICAL CHECK: ensure the canonical jobId slug is mapped, NOT the platform ID
        assertEquals("asb-bank.nz.engineer.2023-01-01", profile.activeRoles[0].id)
        assertEquals("ASB Bank", profile.companyDetails.name)
        assertEquals("Remote", profile.insights.workModel)
    }

    @Test
    fun `mapCompanyProfile handles empty results gracefully`() {
        val emptyDet = mockk<TableResult>()
        val emptyJobs = mockk<TableResult>()
        val emptyAgg = mockk<TableResult>()

        every { emptyDet.values } returns emptyList()
        every { emptyJobs.values } returns emptyList()
        every { emptyAgg.values } returns emptyList()

        val profile = CompanyMapper.mapCompanyProfile("unknown", emptyDet, emptyJobs, emptyAgg)

        assertEquals("Unknown Company", profile.companyDetails.name)
        assertEquals(0, profile.activeRoles.size)
    }

    @Test
    fun `mapCompanyProfile handles null optional fields in detail row`() {
        val detResult = mockk<TableResult>()
        val jobsResult = mockk<TableResult>()
        val aggResult = mockk<TableResult>()

        val detRow = mockk<FieldValueList>()

        val nameVal = mockk<FieldValue>()
        every { nameVal.stringValue } returns "Minimal Corp"
        every { detRow.get(CompanyFields.NAME) } returns nameVal

        // All optional fields are null
        val nullField = mockk<FieldValue>()
        every { nullField.isNull } returns true
        every { detRow.get(CompanyFields.LOGO_URL) } returns nullField
        every { detRow.get(CompanyFields.WEBSITE) } returns nullField
        every { detRow.get(CompanyFields.EMPLOYEES_COUNT) } returns nullField
        every { detRow.get(CompanyFields.INDUSTRIES) } returns nullField
        every { detRow.get(CompanyFields.DESCRIPTION) } returns nullField
        every { detRow.get(CompanyFields.TECHNOLOGIES) } returns nullField
        every { detRow.get(CompanyFields.HIRING_LOCATIONS) } returns nullField

        every { detResult.values } returns listOf(detRow)
        every { jobsResult.values } returns emptyList()
        every { aggResult.values } returns emptyList()

        val profile =
                CompanyMapper.mapCompanyProfile("minimal-corp", detResult, jobsResult, aggResult)

        assertEquals("Minimal Corp", profile.companyDetails.name)
        assertEquals("", profile.companyDetails.logo)
        assertEquals("", profile.companyDetails.website)
        assertEquals(0, profile.companyDetails.employeesCount)
        assertEquals(0, profile.activeRoles.size)
    }
}
