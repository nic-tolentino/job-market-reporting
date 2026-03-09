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
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/internal/**").permitAll()
                    .requestMatchers("/api/webhook/**").permitAll()
                    .requestMatchers("/api/admin/**").permitAll()
                    .requestMatchers("/api/**").permitAll()
                    .anyRequest().authenticated()
            }
            .csrf { csrf ->
                csrf.ignoringRequestMatchers("/api/**", "/api/internal/**", "/api/webhook/**", "/api/admin/**")
            }

        return http.build()
    }
}
