package com.techmarket.sync

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.techmarket.persistence.company.CompanyRepository
import com.techmarket.persistence.model.CompanyRecord
import com.techmarket.sync.model.CompanyJsonDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.time.Instant

/**
 * Service to synchronize the Master Manifest company data from the local JSON file to the
 * Silver layer in BigQuery.
 */
@Service
class CompanySyncService(
    private val companyRepository: CompanyRepository,
    private val objectMapper: ObjectMapper,
    @Value("\${tech-market.data-path:data/companies.json}")
    private val dataPath: String
) {

    private val log = LoggerFactory.getLogger(CompanySyncService::class.java)

    /**
     * Reads the companies.json file and performs a full refresh of the BigQuery companies table.
     * This ensures the definitive curated data (Master Manifest) is always used as the source of truth.
     */
    fun syncFromManifest() {
        log.info("Starting Company Manifest Sync from: $dataPath")
        
        val file = File(dataPath)
        if (!file.exists()) {
            throw IllegalArgumentException("Company manifest JSON not found: $dataPath")
        }

        val companyDtos: List<CompanyJsonDto> = objectMapper.readValue(
            file,
            object : TypeReference<List<CompanyJsonDto>>() {}
        )

        val records = companyDtos.map { dto ->
            CompanyRecord(
                companyId = dto.id,
                name = dto.name,
                alternateNames = dto.alternateNames,
                logoUrl = dto.logoUrl,
                description = dto.description,
                website = dto.website,
                employeesCount = dto.employeesCount,
                industries = dto.industries.joinToString(", "),
                technologies = emptyList(), // Filled during job sync
                hiringLocations = emptyList(), // Filled during job sync
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

        log.info("Parsed \${records.size} Master Manifest company records. Refreshing BigQuery table...")
        
        // Wipe then insert is simpler and safer for the master sync
        companyRepository.deleteAllCompanies()
        companyRepository.saveCompanies(records)
        
        log.info("Company Manifest Sync completed successfully.")
    }
}
