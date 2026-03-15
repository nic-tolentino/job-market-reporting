package com.techmarket.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

/**
 * Interceptor that validates the administrative token for /api/admin/* routes.
 *
 * Supports two authentication modes:
 * 1. `Authorization: Bearer <token>` header (Standard API calls)
 * 2. `?token=<token>` query parameter (Server-Sent Events / EventSource)
 *
 * Implement a fail-closed policy: if `ADMIN_PANEL_TOKEN` is blank or missing from environment,
 * all authenticated requests will be rejected even if a token is provided.
 */
@Component
class AdminTokenInterceptor(
    @Value("\${ADMIN_PANEL_TOKEN:}") private val adminToken: String
) : HandlerInterceptor {

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any
    ): Boolean {
        val authHeader = request.getHeader("Authorization")
        val queryToken = request.getParameter("token")
        
        val isAuthorized = when {
            adminToken.isBlank() -> false
            authHeader == "Bearer $adminToken" -> true
            queryToken == adminToken -> true
            else -> false
        }

        if (isAuthorized) {
            return true
        }

        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.writer.write("{\"error\": \"Unauthorized\"}")
        response.writer.flush()
        return false
    }
}
