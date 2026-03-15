package com.techmarket.sync

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techmarket.persistence.ats.AtsConfigRepository
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.crawler.CrawlerSeedRepository
import com.techmarket.persistence.model.CompanyRecord
import io.mockk.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.nio.file.Files

class CompanySyncServiceTest {

    private val repository = mockk<CompanyRepository>(relaxed = true)
    private val atsRepository = mockk<AtsConfigRepository>(relaxed = true)
    private val crawlerSeedRepository = mockk<CrawlerSeedRepository>(relaxed = true)
    private val mapper = jacksonObjectMapper()
    
    @Test
    fun `syncFromManifest wipes and saves records from directory`() {
        // Create temp directory
        val tempDir = Files.createTempDirectory("companies").toFile()
        val companyFile = File(tempDir, "test-id.json")
        companyFile.writeText("""
            {
              "id": "test-id",
              "name": "Test Company",
              "is_agency": true,
              "hq_country": "NZ",
              "verification_level": "verified",
              "industries": ["Tech"]
            }
        """.trimIndent())

        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, tempDir.absolutePath)
        
        service.syncFromManifest()

        verify { repository.deleteAllCompanies() }
        verify { 
            repository.saveCompanies(match { 
                it.size == 1 && it[0].companyId == "test-id" && it[0].isAgency && it[0].hqCountry == "NZ"
            }) 
        }
        
        tempDir.deleteRecursively()
    }

    @Test
    fun `syncFromManifest finds files in nested directories`() {
        val tempDir = Files.createTempDirectory("nested-companies").toFile()
        val subDir = File(tempDir, "t").apply { mkdir() }
        val companyFile = File(subDir, "test-id.json")
        companyFile.writeText("""
            {
              "id": "test-id",
              "name": "Nested Company",
              "is_agency": false,
              "hq_country": "AU",
              "verification_level": "silver",
              "industries": ["Software"]
            }
        """.trimIndent())

        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, tempDir.absolutePath)
        
        service.syncFromManifest()

        verify { 
            repository.saveCompanies(match { 
                it.size == 1 && it[0].companyId == "test-id" && it[0].hqCountry == "AU"
            }) 
        }
        
        tempDir.deleteRecursively()
    }

    @Test
    fun `syncFromManifest throws exception if directory not found`() {
        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, "missing-dir")
        assertThrows<IllegalArgumentException> {
            service.syncFromManifest()
        }
    }
}
