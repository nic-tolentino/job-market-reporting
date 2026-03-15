package com.techmarket.config

import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class AdminTokenInterceptorTest {

    private lateinit var interceptor: AdminTokenInterceptor
    private val adminToken = "test-token"

    @BeforeEach
    fun setup() {
        interceptor = AdminTokenInterceptor(adminToken)
    }

    @Test
    fun `preHandle returns true when token matches`() {
        val request = MockHttpServletRequest()
        request.requestURI = "/api/admin/crawler/health"
        request.addHeader("Authorization", "Bearer $adminToken")
        val response = MockHttpServletResponse()

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        assertEquals(HttpServletResponse.SC_OK, response.status)
    }

    @Test
    fun `preHandle returns true when query token matches`() {
        val request = MockHttpServletRequest()
        request.requestURI = "/api/admin/crawler/logs"
        request.addParameter("token", adminToken)
        val response = MockHttpServletResponse()

        val result = interceptor.preHandle(request, response, Any())

        assertTrue(result)
        assertEquals(HttpServletResponse.SC_OK, response.status)
    }

    @Test
    fun `preHandle returns false and 401 when token is missing`() {
        val request = MockHttpServletRequest()
        request.requestURI = "/api/admin/crawler/health"
        val response = MockHttpServletResponse()

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `preHandle returns false and 401 when token is incorrect`() {
        val request = MockHttpServletRequest()
        request.requestURI = "/api/admin/crawler/health"
        request.addHeader("Authorization", "Bearer wrong-token")
        val response = MockHttpServletResponse()

        val result = interceptor.preHandle(request, response, Any())

        assertFalse(result)
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }

    @Test
    fun `preHandle returns false when adminToken is blank`() {
        val blankInterceptor = AdminTokenInterceptor("")
        val request = MockHttpServletRequest()
        request.addHeader("Authorization", "Bearer matches-nothing")
        val response = MockHttpServletResponse()

        val result = blankInterceptor.preHandle(request, response, Any())

        assertFalse(result)
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.status)
    }
}
