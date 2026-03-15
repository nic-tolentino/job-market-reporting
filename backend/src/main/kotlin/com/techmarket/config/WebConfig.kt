package com.techmarket.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.filter.CommonsRequestLoggingFilter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val adminTokenInterceptor: AdminTokenInterceptor
) : WebMvcConfigurer {

    override fun addInterceptors(registry: org.springframework.web.servlet.config.annotation.InterceptorRegistry) {
        registry.addInterceptor(adminTokenInterceptor)
            .addPathPatterns("/api/admin/**")
    }

    override fun addCorsMappings(registry: CorsRegistry) {
        val extraOrigins =
                System.getenv("CORS_ALLOWED_ORIGINS")?.split(",")?.toTypedArray() ?: emptyArray()

        registry.addMapping("/api/**")
                .allowedOrigins(
                        "http://localhost:5173", // Local frontend
                        "https://devassembly.org", // Production
                        *extraOrigins
                )
                .allowedOriginPatterns("https://*.vercel.app") // Vercel Preview Deployments
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
    }

    @Bean
    fun logFilter(): CommonsRequestLoggingFilter {
        val filter = CommonsRequestLoggingFilter()
        filter.setIncludeQueryString(true)
        filter.setIncludeClientInfo(true)
        filter.setIncludeHeaders(false)
        filter.setIncludePayload(false)
        filter.setAfterMessagePrefix("API REQUEST: ")
        return filter
    }
}
