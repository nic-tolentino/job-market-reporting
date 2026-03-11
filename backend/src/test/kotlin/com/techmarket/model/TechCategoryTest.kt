package com.techmarket.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.server.ResponseStatusException

class TechCategoryTest {

    @Test
    fun `fromSlug returns correct category for valid slugs`() {
        assertEquals(TechCategory.MOBILE, TechCategory.fromSlug("mobile"))
        assertEquals(TechCategory.CLOUD_INFRA, TechCategory.fromSlug("cloud-infra"))
        assertEquals(TechCategory.FRONTEND, TechCategory.fromSlug("frontend"))
    }

    @Test
    fun `fromSlug throws 404 for unknown slug`() {
        val exception = assertThrows<ResponseStatusException> {
            TechCategory.fromSlug("unknown")
        }
        assertEquals(404, exception.statusCode.value())
    }

    @Test
    fun `fromSlug is case sensitive and throws 404 for uppercase`() {
        val exception = assertThrows<ResponseStatusException> {
            TechCategory.fromSlug("FRONTEND")
        }
        assertEquals(404, exception.statusCode.value())
    }
}
