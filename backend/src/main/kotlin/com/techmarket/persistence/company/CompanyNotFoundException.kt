package com.techmarket.persistence.company

/**
 * Exception thrown when a requested company is not found in the database.
 */
class CompanyNotFoundException(companyId: String) : 
    RuntimeException("Company not found: $companyId")
