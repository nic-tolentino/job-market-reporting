package com.techmarket.sync

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.model.CompanyRecord
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files

class CompanySyncServiceTest {

    private val repository = mockk<CompanyRepository>(relaxed = true)
    private val mapper = jacksonObjectMapper()
    
    @Test
    fun `syncFromManifest wipes and saves records from JSON`() {
        // Create temp JSON file
        val tempFile = Files.createTempFile("companies", ".json").toFile()
        tempFile.writeText("""
            [
              {
                "id": "test-id",
                "name": "Test Company",
                "is_agency": true,
                "hq_country": "NZ"
              }
            ]
        """.trimIndent())

        val service = CompanySyncService(repository, mapper, tempFile.absolutePath)
        
        service.syncFromManifest()

        verify { repository.deleteAllCompanies() }
        verify { 
            repository.saveCompanies(match { 
                it.size == 1 && it[0].companyId == "test-id" && it[0].isAgency && it[0].hqCountry == "NZ"
            }) 
        }
        
        tempFile.delete()
    }

    @Test
    fun `syncFromManifest throws exception if file not found`() {
        val service = CompanySyncService(repository, mapper, "missing.json")
        assertThrows<IllegalArgumentException> {
            service.syncFromManifest()
        }
    }
}
