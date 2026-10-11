package com.momentlink.api.auth.common.security

import com.momentlink.api.participant.security.ParticipantSessionAuthFilter
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val participantSessionAuthFilter: ParticipantSessionAuthFilter,
) {
    private val WHITELIST = arrayOf(
        // swagger
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/v3/api-docs",
        "/v3/api-docs/**",

        // api
        "/api/v1/auth/login",
        "/api/v1/auth/callback/**",
        "/api/v1/auth/token/refresh",
        "/error",


    )

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .cors { cors -> cors.configurationSource(corsConfigurationSource()) }
            .csrf { csrf -> csrf.disable() }
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .formLogin { formLogin -> formLogin.disable() }
            .httpBasic { httpBasic -> httpBasic.disable() }
            .logout { logout -> logout.disable() }
            .authorizeHttpRequests { authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/api/dev/events").permitAll()
                    .requestMatchers("/api/dev/**").denyAll()
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(*WHITELIST).permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/v1/events/access").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/v1/participant/*/session").permitAll()
                    .requestMatchers("/api/v1/participant/**").hasRole("PARTICIPANT")

                    // 나머지 호스트는 JWT 인증 사용
                    .anyRequest()
                    .hasRole("HOST")
            }

            .exceptionHandling { exception ->
                exception
                    .authenticationEntryPoint { _, response, _ ->
                        response.sendError(
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "인증이 필요합니다.",
                        )
                    }
                    .accessDeniedHandler { _, response, _ ->
                        response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "접근 권한이 없습니다.",
                        )
                    }
            }

        http
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(participantSessionAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = listOf(
                "http://localhost:5173",
            )

            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf(
                "Authorization",
                "Content-Type",
                "X-Event-Access-Key",
                "Idempotency-Key",
            )

            allowCredentials = true
            maxAge = 3600L
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", config)
        }
    }

    @Bean
    fun jwtAuthFilterRegistration(
        filter: JwtAuthFilter,
    ): FilterRegistrationBean<JwtAuthFilter> =
        FilterRegistrationBean(filter).apply {
            isEnabled = false
        }

    @Bean
    fun participantSessionAuthFilterRegistration(
        filter: ParticipantSessionAuthFilter,
    ): FilterRegistrationBean<ParticipantSessionAuthFilter> =
        FilterRegistrationBean(filter).apply {
            isEnabled = false
        }

}
