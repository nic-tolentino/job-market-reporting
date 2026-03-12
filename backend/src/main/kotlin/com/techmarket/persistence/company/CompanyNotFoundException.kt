package com.techmarket.persistence.company

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

/**
 * Exception thrown when a requested company is not found in the database.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
class CompanyNotFoundException(companyId: String) :
    RuntimeException("Company not found: $companyId")
