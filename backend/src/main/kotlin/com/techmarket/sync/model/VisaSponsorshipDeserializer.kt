package com.techmarket.sync.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.techmarket.models.VisaSponsorshipInfo

class VisaSponsorshipDeserializer : JsonDeserializer<VisaSponsorshipInfo?>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): VisaSponsorshipInfo? {
        return when (val token = p.currentToken()) {
            JsonToken.VALUE_TRUE -> VisaSponsorshipInfo(offered = true)
            JsonToken.VALUE_FALSE -> VisaSponsorshipInfo(offered = false)
            JsonToken.VALUE_NULL -> null
            JsonToken.START_OBJECT, JsonToken.FIELD_NAME -> {
                val node: JsonNode = p.codec.readTree(p)
                val offered = node.get("offered")?.asBoolean() ?: false
                val types = node.get("types")?.map { it.asText() } ?: emptyList()
                val notes = node.get("notes")?.asText()
                val lastVerified = node.get("last_verified")?.asText() ?: node.get("lastVerified")?.asText()
                val source = node.get("source")?.asText()
                VisaSponsorshipInfo(offered, types, notes, lastVerified, source)
            }
            else -> throw ctxt.wrongTokenException(
                p, 
                VisaSponsorshipInfo::class.java, 
                token, 
                "Expected boolean or object for visa_sponsorship"
            )
        }
    }
}
