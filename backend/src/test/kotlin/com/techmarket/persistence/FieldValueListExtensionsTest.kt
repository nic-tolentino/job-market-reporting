package com.techmarket.persistence

import com.google.cloud.bigquery.FieldValue
import com.google.cloud.bigquery.FieldValueList
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * Tests for FieldValueListExtensions null-safety and exception handling.
 * 
 * These tests verify that the extension functions gracefully handle
 * IllegalArgumentException when a field is not in the SELECT clause,
 * which is the critical safety mechanism for partial queries.
 */
class FieldValueListExtensionsTest {

    @Test
    fun `getStringOrDefault returns default when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getStringOrDefault("unknown_field", "default_value")

        assertEquals("default_value", result)
    }

    @Test
    fun `getStringOrNull returns null when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getStringOrNull("unknown_field")

        assertEquals(null, result)
    }

    @Test
    fun `getString returns empty string when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getString("unknown_field")

        assertEquals("", result)
    }

    @Test
    fun `getLongOrNull returns null when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getLongOrNull("unknown_field")

        assertEquals(null, result)
    }

    @Test
    fun `getBooleanOrDefault returns default when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getBooleanOrDefault("unknown_field", true)

        assertEquals(true, result)
    }

    @Test
    fun `getBoolean returns false when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getBoolean("unknown_field")

        assertEquals(false, result)
    }

    @Test
    fun `getBoolean returns false when field is null`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        every { field.isNull } returns true
        every { fieldList.get("null_field") } returns field

        val result = fieldList.getBoolean("null_field")

        assertEquals(false, result)
    }

    @Test
    fun `getBoolean returns value when field exists and is not null`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        every { field.booleanValue } returns true
        every { fieldList.get("known_field") } returns field

        val result = fieldList.getBoolean("known_field")

        assertEquals(true, result)
    }

    @Test
    fun `getStringList returns empty list when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getStringList("unknown_field")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getStringListOrNull returns empty list when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getStringListOrNull("unknown_field")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getTimestamp returns EPOCH when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getTimestamp("unknown_field")

        assertEquals(Instant.EPOCH, result)
    }

    @Test
    fun `getTimestampOrDefault returns default when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        val customInstant = Instant.parse("2026-01-01T00:00:00Z")
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getTimestampOrDefault("unknown_field", customInstant)

        assertEquals(customInstant, result)
    }

    @Test
    fun `getSalaryOrNull returns null when field throws IllegalArgumentException`() {
        val fieldList = mockk<FieldValueList>()
        every { fieldList.get("unknown_field") } throws IllegalArgumentException("Field not found")

        val result = fieldList.getSalaryOrNull("unknown_field")

        assertEquals(null, result)
    }

    @Test
    fun `getString returns empty string when field is null`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        every { field.isNull } returns true
        every { fieldList.get("null_field") } returns field

        val result = fieldList.getString("null_field")

        assertEquals("", result)
    }

    @Test
    fun `getString returns value when field exists and is not null`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        every { field.stringValue } returns "test_value"
        every { fieldList.get("known_field") } returns field

        val result = fieldList.getString("known_field")

        assertEquals("test_value", result)
    }

    @Test
    fun `getStringOrDefault returns value when field exists`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        every { field.isNull } returns false
        every { field.stringValue } returns "test_value"
        every { fieldList.get("known_field") } returns field

        val result = fieldList.getStringOrDefault("known_field", "default")

        assertEquals("test_value", result)
    }

    @Test
    fun `getStringOrDefault returns default when field is null`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        every { field.isNull } returns true
        every { fieldList.get("null_field") } returns field

        val result = fieldList.getStringOrDefault("null_field", "default_value")

        assertEquals("default_value", result)
    }

    @Test
    fun `getStringList returns empty list when field is null`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        every { field.isNull } returns true
        every { fieldList.get("null_field") } returns field

        val result = fieldList.getStringList("null_field")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getStringList returns values when field has repeated values`() {
        val fieldList = mockk<FieldValueList>()
        val field = mockk<FieldValue>()
        val value1 = mockk<FieldValue>()
        val value2 = mockk<FieldValue>()
        every { value1.isNull } returns false
        every { value1.stringValue } returns "value1"
        every { value2.isNull } returns false
        every { value2.stringValue } returns "value2"
        every { field.isNull } returns false
        every { field.repeatedValue } returns listOf(value1, value2)
        every { fieldList.get("list_field") } returns field

        val result = fieldList.getStringList("list_field")

        assertEquals(listOf("value1", "value2"), result)
    }
}
