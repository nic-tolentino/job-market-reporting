package com.techmarket.persistence

import com.google.cloud.bigquery.Field
import com.google.cloud.bigquery.FieldList
import com.google.cloud.bigquery.FieldValueList
import com.google.cloud.bigquery.StandardSQLTypeName
import com.techmarket.model.NormalizedSalary
import com.techmarket.persistence.SalaryFields.AMOUNT
import com.techmarket.persistence.SalaryFields.CURRENCY
import com.techmarket.persistence.SalaryFields.IS_GROSS
import com.techmarket.persistence.SalaryFields.PERIOD
import com.techmarket.persistence.SalaryFields.SOURCE

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
                Field.of(AMOUNT, StandardSQLTypeName.INT64),
                Field.of(CURRENCY, StandardSQLTypeName.STRING),
                Field.of(PERIOD, StandardSQLTypeName.STRING),
                Field.of(SOURCE, StandardSQLTypeName.STRING),
                Field.of(IS_GROSS, StandardSQLTypeName.BOOL)
            )
        )
    }

    /**
     * Extracts a NormalizedSalary from a BigQuery FieldValueList.
     * Uses safe null handling for nested STRUCT fields.
     * 
     * @param fieldValueList The BigQuery result row
     * @param fieldName The name of the salary field (e.g., "salaryMin", "salaryMax")
     * @return NormalizedSalary or null if the field is null
     */
    fun fromFieldValue(fieldValueList: FieldValueList, fieldName: String): NormalizedSalary? {
        if (fieldValueList.get(fieldName).isNull) return null

        val salaryStruct = fieldValueList.get(fieldName).recordValue
        if (salaryStruct == null) return null
        
        // Safe extraction with defaults for nullable nested fields
        val amount = salaryStruct.get(AMOUNT).takeIf { !it.isNull }?.longValue ?: return null
        val currency = salaryStruct.get(CURRENCY).takeIf { !it.isNull }?.stringValue ?: NormalizedSalary.CURRENCY_NZD
        val period = salaryStruct.get(PERIOD).takeIf { !it.isNull }?.stringValue ?: NormalizedSalary.PERIOD_YEAR
        val source = salaryStruct.get(SOURCE).takeIf { !it.isNull }?.stringValue ?: NormalizedSalary.SOURCE_JOB_POSTING
        val isGross = salaryStruct.get(IS_GROSS).takeIf { !it.isNull }?.booleanValue ?: true
        
        return NormalizedSalary(amount, currency, period, source, isGross)
    }
}
