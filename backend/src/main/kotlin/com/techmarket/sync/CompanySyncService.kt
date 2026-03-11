package com.techmarket.sync

import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.ats.AtsConfigRepository
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.model.CompanyAtsConfig
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.sync.ats.AtsProvider
import com.techmarket.sync.ats.SyncStatus
import com.techmarket.sync.model.AtsConfigDto
import com.techmarket.sync.model.CompanyJsonDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.streams.toList

@Service
class CompanySyncService(
    private val companyRepository: CompanyRepository,
    private val atsConfigRepository: AtsConfigRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${tech-market.manifest-dir:data/companies}")
    private val manifestDir: String
) {

    private val log = LoggerFactory.getLogger(CompanySyncService::class.java)

    /**
     * Reads all JSON files in the manifest directory and performs a full refresh of the BigQuery companies table.
     * Also syncs ATS configurations to the ats_configs table.
     */
    fun syncFromManifest() {
        log.info("Starting Company Manifest Sync from directory: $manifestDir")

        val dir = File(manifestDir)
        if (!dir.exists() || !dir.isDirectory) {
            throw IllegalArgumentException("Company manifest directory not found: $manifestDir")
        }

        val jsonFiles = Files.walk(dir.toPath())
            .filter { path ->
                val file = path.toFile()
                file.isFile && file.extension == "json" && file.name != "schema.json" && !file.name.startsWith(".")
            }
            .map { it.toFile() }
            .toList()

        if (jsonFiles.isEmpty()) {
            log.warn("No company JSON files found in $manifestDir")
            return
        }

        val companies = jsonFiles.sortedBy { it.name }.mapNotNull { file ->
            try {
                objectMapper.readValue(file, CompanyJsonDto::class.java)
            } catch (e: Exception) {
                log.error("Failed to parse company file ${file.name}: ${e.message}")
                null
            }
        }

        val records = companies.map { dto: CompanyJsonDto ->
            CompanyRecord(
                companyId = dto.id,
                name = dto.name,
                alternateNames = dto.alternateNames,
                logoUrl = dto.logoUrl,
                description = dto.description,
                website = dto.website,
                employeesCount = dto.employeesCount,
                industries = dto.industries.joinToString(", "),
                technologies = emptyList(),
                hiringLocations = emptyList(),
                isAgency = dto.isAgency,
                isSocialEnterprise = dto.isSocialEnterprise,
                hqCountry = dto.hqCountry,
                operatingCountries = dto.operatingCountries,
                officeLocations = dto.officeLocations,
                remotePolicy = dto.remotePolicy,
                visaSponsorship = dto.visaSponsorship,
                verificationLevel = dto.verificationLevel,
                lastUpdatedAt = Instant.now()
            )
        }

        log.info("Parsed ${records.size} Master Manifest company records from directory. Refreshing BigQuery table...")

        companyRepository.deleteAllCompanies()
        companyRepository.saveCompanies(records)

        log.info("Company records synced. Now syncing ATS configurations...")

        // Sync ATS configurations
        syncAtsConfigurations(companies)

        log.info("Company Manifest Sync completed successfully.")
    }

    /**
     * Syncs ATS configurations from manifest to BigQuery.
     * Preserves operational state (enabled, syncStatus) for existing configs.
     */
    private fun syncAtsConfigurations(companies: List<CompanyJsonDto>) {
        var saved = 0
        var skipped = 0

        companies.forEach { dto ->
            dto.atsConfig?.let { atsDto ->
                try {
                    val existingConfig = atsConfigRepository.getConfig(dto.id)
                    
                    val config = CompanyAtsConfig(
                        companyId = dto.id,
                        atsProvider = AtsProvider.valueOf(atsDto.provider.uppercase()),
                        identifier = atsDto.identifier,
                        enabled = existingConfig?.enabled ?: true,  // Preserve enabled state or default to true
                        lastSyncedAt = existingConfig?.lastSyncedAt,  // Preserve last sync time
                        syncStatus = existingConfig?.syncStatus ?: SyncStatus.PENDING  // Preserve sync status
                    )

                    atsConfigRepository.saveConfig(config)
                    saved++
                    log.debug("Saved ATS config for ${dto.id}: ${atsDto.provider}/${atsDto.identifier}")
                    
                } catch (e: Exception) {
                    log.error("Failed to save ATS config for ${dto.id}: ${e.message}", e)
                }
            } ?: run {
                // Company has no ATS config in manifest
                // Don't delete existing config - preserve operational state
                skipped++
            }
        }

        log.info("ATS configurations synced: $saved saved, ${companies.size - saved} without ATS config")
    }
}
