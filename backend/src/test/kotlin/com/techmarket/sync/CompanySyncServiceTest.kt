package com.techmarket.sync

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.techmarket.persistence.ats.AtsConfigRepository
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.crawler.CrawlerSeedRepository
import com.techmarket.persistence.model.CompanyRecord
import io.mockk.*
import io.mockk.every
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

    @Test
    fun `syncFromManifest saves ATS config when manifest has ats field`() {
        val tempDir = Files.createTempDirectory("ats-companies").toFile()
        File(tempDir, "acme.json").writeText("""
            {
              "id": "acme",
              "name": "Acme Corp",
              "verification_level": "unverified",
              "ats": { "provider": "GREENHOUSE", "identifier": "acme" }
            }
        """.trimIndent())

        every { atsRepository.getConfig("acme") } returns null

        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, tempDir.absolutePath)
        service.syncFromManifest()

        verify {
            atsRepository.saveConfig(match {
                it.companyId == "acme" &&
                it.identifier == "acme" &&
                it.atsProvider.name == "GREENHOUSE" &&
                it.enabled
            })
        }

        tempDir.deleteRecursively()
    }

    @Test
    fun `syncFromManifest saves Lever ATS config correctly`() {
        val tempDir = Files.createTempDirectory("lever-companies").toFile()
        File(tempDir, "startup.json").writeText("""
            {
              "id": "startup",
              "name": "Startup Inc",
              "verification_level": "unverified",
              "ats": { "provider": "LEVER", "identifier": "startup-inc" }
            }
        """.trimIndent())

        every { atsRepository.getConfig("startup") } returns null

        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, tempDir.absolutePath)
        service.syncFromManifest()

        verify {
            atsRepository.saveConfig(match {
                it.companyId == "startup" &&
                it.identifier == "startup-inc" &&
                it.atsProvider.name == "LEVER"
            })
        }

        tempDir.deleteRecursively()
    }

    @Test
    fun `syncFromManifest saves Ashby ATS config correctly`() {
        val tempDir = Files.createTempDirectory("ashby-companies").toFile()
        File(tempDir, "scale.json").writeText("""
            {
              "id": "scale",
              "name": "Scale AI",
              "verification_level": "unverified",
              "ats": { "provider": "ASHBY", "identifier": "scale" }
            }
        """.trimIndent())

        every { atsRepository.getConfig("scale") } returns null

        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, tempDir.absolutePath)
        service.syncFromManifest()

        verify {
            atsRepository.saveConfig(match {
                it.companyId == "scale" &&
                it.identifier == "scale" &&
                it.atsProvider.name == "ASHBY"
            })
        }

        tempDir.deleteRecursively()
    }

    @Test
    fun `syncFromManifest preserves enabled state and sync status for existing ATS configs`() {
        val tempDir = Files.createTempDirectory("preserve-companies").toFile()
        File(tempDir, "existing.json").writeText("""
            {
              "id": "existing",
              "name": "Existing Co",
              "verification_level": "unverified",
              "ats": { "provider": "GREENHOUSE", "identifier": "existing" }
            }
        """.trimIndent())

        val existingConfig = com.techmarket.persistence.model.CompanyAtsConfig(
            companyId   = "existing",
            atsProvider = com.techmarket.sync.ats.AtsProvider.GREENHOUSE,
            identifier  = "existing",
            enabled     = false,   // explicitly disabled
            lastSyncedAt = null,
            syncStatus  = com.techmarket.sync.ats.SyncStatus.FAILED
        )
        every { atsRepository.getConfig("existing") } returns existingConfig

        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, tempDir.absolutePath)
        service.syncFromManifest()

        // Must preserve disabled state and FAILED status rather than resetting them
        verify {
            atsRepository.saveConfig(match {
                !it.enabled && it.syncStatus == com.techmarket.sync.ats.SyncStatus.FAILED
            })
        }

        tempDir.deleteRecursively()
    }

    @Test
    fun `syncFromManifest skips ATS save for companies without ats field`() {
        val tempDir = Files.createTempDirectory("no-ats-companies").toFile()
        File(tempDir, "noats.json").writeText("""
            {
              "id": "noats",
              "name": "No ATS Corp",
              "verification_level": "unverified"
            }
        """.trimIndent())

        val service = CompanySyncService(repository, atsRepository, crawlerSeedRepository, mapper, tempDir.absolutePath)
        service.syncFromManifest()

        verify(exactly = 0) { atsRepository.saveConfig(any()) }

        tempDir.deleteRecursively()
    }
}
