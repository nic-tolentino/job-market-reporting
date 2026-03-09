package com.techmarket.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // Completely disable CSRF for stateless REST API
            .csrf { it.disable() }
            // Allow all API endpoints - controllers handle their own validation
            .authorizeHttpRequests { auth ->
                auth
                    .anyRequest().permitAll()
            }

        return http.build()
    }
}
