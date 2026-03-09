package com.techmarket.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(0)  // Run before Spring Security
class WebhookBypassFilter : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Bypass security for webhook endpoints
        if (request.requestURI.startsWith("/api/webhook/")) {
            filterChain.doFilter(request, response)
            return
        }
        
        // Bypass security for internal Cloud Tasks endpoint
        if (request.requestURI.startsWith("/api/internal/")) {
            filterChain.doFilter(request, response)
            return
        }
        
        // Bypass security for admin endpoints
        if (request.requestURI.startsWith("/api/admin/")) {
            filterChain.doFilter(request, response)
            return
        }
        
        filterChain.doFilter(request, response)
    }
}
