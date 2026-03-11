package com.techmarket.sync.model

import com.techmarket.models.VisaSponsorshipInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class VisaSponsorshipDeserializerTest {
    private val mapper: ObjectMapper = jacksonObjectMapper()

    private fun deserialize(json: String): VisaSponsorshipInfo? {
        val dto = mapper.readValue("""{"visa_sponsorship": $json}""", TestDto::class.java)
        return dto.visa_sponsorship
    }

    private data class TestDto(
        @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = VisaSponsorshipDeserializer::class)
        val visa_sponsorship: VisaSponsorshipInfo?
    )

    @Test
    fun `deserializes boolean true as offered`() {
        val result = deserialize("true")
        assertEquals(true, result?.offered)
        assertEquals(emptyList<String>(), result?.types)
    }

    @Test
    fun `deserializes boolean false as not offered`() {
        val result = deserialize("false")
        assertEquals(false, result?.offered)
    }

    @Test
    fun `deserializes full object with types and notes`() {
        val json = """{"offered": true, "types": ["Talent"], "notes": "Fast-track"}"""
        val result = deserialize(json)
        assertEquals(true, result?.offered)
        assertEquals(listOf("Talent"), result?.types)
        assertEquals("Fast-track", result?.notes)
    }

    @Test
    fun `deserializes object with offered=false`() {
        val json = """{"offered": false, "notes": "Not currently sponsoring"}"""
        val result = deserialize(json)
        assertEquals(false, result?.offered)
        assertEquals("Not currently sponsoring", result?.notes)
    }

    @Test
    fun `deserializes null as null`() {
        val result = deserialize("null")
        assertNull(result)
    }
}
