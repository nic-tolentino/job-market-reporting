package com.techmarket.persistence

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import com.techmarket.model.NormalizedSalary
import java.time.Instant

/**
 * Extension functions to simplify BigQuery FieldValueList access.
 * Provides sensible defaults and handles null-safety ergonomically.
 * 
 * All functions tolerate missing fields (not in SELECT) by returning defaults
 * instead of throwing IllegalArgumentException.
 */

/**
 * Gets a field value, returning null if the field doesn't exist in the result set.
 */
private fun FieldValueList.getFieldOrNull(field: String): FieldValue? {
    return try {
        get(field)
    } catch (e: IllegalArgumentException) {
        // Field not in SELECT - return null to trigger default
        null
    }
}

fun FieldValueList.getString(field: String): String =
    getStringOrNull(field) ?: ""

fun FieldValueList.getStringOrNull(field: String): String? =
    getFieldOrNull(field)?.takeIf { !it.isNull }?.stringValue

fun FieldValueList.getStringOrDefault(field: String, default: String = ""): String =
    getStringOrNull(field) ?: default

/**
 * Gets a required string field, throwing IllegalStateException if missing or null.
 * Use this for primary key fields where a missing value indicates a broken query
 * (e.g. forgot to include the field in SELECT) rather than legitimate absent data.
 */
fun FieldValueList.getStringOrThrow(field: String): String =
    getStringOrNull(field)
        ?: throw IllegalStateException("Required field '$field' is missing or null in query result. Check that the SQL SELECT includes this field.")

fun FieldValueList.getLongOrNull(field: String): Long? =
    getFieldOrNull(field)?.takeIf { !it.isNull }?.longValue

fun FieldValueList.getBoolean(field: String): Boolean =
    getBooleanOrDefault(field, false)

fun FieldValueList.getBooleanOrDefault(field: String, default: Boolean): Boolean =
    getFieldOrNull(field)?.takeIf { !it.isNull }?.booleanValue ?: default

fun FieldValueList.getStringList(field: String): List<String> =
    getFieldOrNull(field)?.takeIf { !it.isNull }?.repeatedValue?.map { it.stringValue } ?: emptyList()

/**
 * Returns a list of nullable strings from a repeated field.
 * 
 * Note: Despite the "OrNull" suffix, this returns List<String?> (list with nullable elements),
 * not List<String>? (nullable list). It always returns emptyList() when the field is missing
 * or null, never null. The "OrNull" refers to the list *elements* being nullable.
 */
fun FieldValueList.getStringListOrNull(field: String): List<String?> =
    getFieldOrNull(field)?.takeIf { !it.isNull }?.repeatedValue?.map {
        it.takeIf { v -> !v.isNull }?.stringValue
    } ?: emptyList()

fun FieldValueList.getTimestamp(field: String): Instant {
    val fieldValue = getFieldOrNull(field) ?: return Instant.EPOCH
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
    val fieldValue = getFieldOrNull(field) ?: return default
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
