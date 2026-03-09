package com.techmarket.persistence

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.techmarket.model.NormalizedSalary
import java.time.Instant

/**
 * Extension functions to simplify BigQuery FieldValueList access.
 * Provides sensible defaults and handles null-safety ergonomically.
 */
fun FieldValueList.getString(field: String): String =
    get(field).stringValue

fun FieldValueList.getStringOrNull(field: String): String? =
    get(field).takeIf { !it.isNull }?.stringValue

fun FieldValueList.getStringOrDefault(field: String, default: String = ""): String =
    getStringOrNull(field) ?: default

fun FieldValueList.getLongOrNull(field: String): Long? =
    get(field).takeIf { !it.isNull }?.longValue

fun FieldValueList.getBoolean(field: String): Boolean =
    get(field).booleanValue

fun FieldValueList.getBooleanOrDefault(field: String, default: Boolean): Boolean =
    get(field).takeIf { !it.isNull }?.booleanValue ?: default

fun FieldValueList.getStringList(field: String): List<String> =
    get(field).takeIf { !it.isNull }?.repeatedValue?.map { it.stringValue } ?: emptyList()

fun FieldValueList.getStringListOrNull(field: String): List<String?> =
    get(field).takeIf { !it.isNull }?.repeatedValue?.map { 
        it.takeIf { v -> !v.isNull }?.stringValue 
    } ?: emptyList()

fun FieldValueList.getTimestamp(field: String): Instant {
    val fieldValue = get(field)
    if (fieldValue.isNull) return Instant.EPOCH
    
    val stringVal = fieldValue.stringValue
    val doubleVal = stringVal.toDoubleOrNull()
    return if (doubleVal != null) {
        Instant.ofEpochSecond(doubleVal.toLong())
    } else {
        try {
            Instant.parse(stringVal)
        } catch (e: Exception) {
            Instant.EPOCH
        }
    }
}

fun FieldValueList.getTimestampOrDefault(field: String, default: Instant = Instant.EPOCH): Instant {
    val fieldValue = get(field)
    if (fieldValue.isNull) return default
    
    val stringVal = fieldValue.stringValue
    val doubleVal = stringVal.toDoubleOrNull()
    return if (doubleVal != null) {
        Instant.ofEpochSecond(doubleVal.toLong())
    } else {
        try {
            Instant.parse(stringVal)
        } catch (e: Exception) {
            default
        }
    }
}

/**
 * Extracts a NormalizedSalary from a BigQuery STRUCT field.
 * Delegates to existing SalaryMapper for consistency.
 */
fun FieldValueList.getSalaryOrNull(field: String): NormalizedSalary? =
    SalaryMapper.fromFieldValue(this, field)

/**
 * Extension for FieldValue to get string value (used in legacy test compatibility)
 */
fun FieldValue.getString(): String = this.stringValue
