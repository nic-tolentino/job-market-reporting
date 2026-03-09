package com.techmarket.persistence

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.StandardSQLTypeName
import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.SalaryFields

/**
 * Helper object for salary-related BigQuery operations.
 * Reduces code duplication across mappers and repositories.
 */
object SalaryMapper {
    /**
     * Creates the salary STRUCT field schema with all required fields.
     * @param fieldName The name of the field (e.g., "salaryMin", "salaryMax")
     */
    fun createStructField(fieldName: String): Field {
        return Field.of(
            fieldName,
            StandardSQLTypeName.STRUCT,
            FieldList.of(
                Field.of(SalaryFields.AMOUNT, StandardSQLTypeName.INT64),
                Field.of(SalaryFields.CURRENCY, StandardSQLTypeName.STRING),
                Field.of(SalaryFields.PERIOD, StandardSQLTypeName.STRING),
                Field.of(SalaryFields.SOURCE, StandardSQLTypeName.STRING),
                Field.of(SalaryFields.IS_GROSS, StandardSQLTypeName.BOOL)
            )
        )
    }

    /**
     * Extracts a NormalizedSalary from a BigQuery FieldValueList.
     * Uses safe null handling for nested STRUCT fields.
     * 
     * For international job data:
     * - amount is REQUIRED and must be in cents/smallest currency unit
     * - currency defaults to the job's country currency if missing
     * - period is REQUIRED - can't safely default (YEAR vs HOUR is a big difference!)
     * - source is REQUIRED - should always be provided by the ingestion pipeline
     * - isGross defaults to true (standard convention)
     * 
     * @param fieldValueList The BigQuery result row
     * @param fieldName The name of the salary field (e.g., "salaryMin", "salaryMax")
     * @param jobCountry Optional ISO country code (e.g., "NZ", "AU", "US") for currency default
     * @return NormalizedSalary or null if the field is null, amount is missing, or period is missing
     */
    fun fromFieldValue(
        fieldValueList: FieldValueList, 
        fieldName: String,
        jobCountry: String? = null
    ): NormalizedSalary? {
        if (fieldValueList.get(fieldName).isNull) return null

        val salaryStruct = fieldValueList.get(fieldName).recordValue
        if (salaryStruct == null) return null
        
        // amount is REQUIRED - a salary without an amount is meaningless
        val amount = salaryStruct.get(SalaryFields.AMOUNT).takeIf { !it.isNull }?.longValue ?: return null
        
        // currency: use country-based default if missing
        val currency = salaryStruct.get(SalaryFields.CURRENCY).takeIf { !it.isNull }?.stringValue
            ?: jobCountry?.let { NormalizedSalary.getDefaultCurrencyForCountry(it) }
            ?: return null  // Can't determine currency
        
        // period is REQUIRED - too risky to default (YEAR vs HOUR is 2000x difference!)
        val period = salaryStruct.get(SalaryFields.PERIOD).takeIf { !it.isNull }?.stringValue ?: return null
        
        // source is REQUIRED - ingestion pipeline should always provide this
        val source = salaryStruct.get(SalaryFields.SOURCE).takeIf { !it.isNull }?.stringValue ?: return null
        
        // isGross defaults to true (standard convention for most markets)
        val isGross = salaryStruct.get(SalaryFields.IS_GROSS).takeIf { !it.isNull }?.booleanValue ?: true
        
        return NormalizedSalary(amount, currency, period, source, isGross)
    }
}
