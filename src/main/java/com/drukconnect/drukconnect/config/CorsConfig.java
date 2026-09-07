package com.drukconnect.drukconnect.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * =========================================================
         * FRONTEND ORIGINS
         * =========================================================
         *
         * Local Angular:
         * http://localhost:4200
         *
         * Add production frontend URL later.
         */
        configuration.setAllowedOrigins(
                List.of(
                        "http://localhost:3000"
                )
        );

        /*
         * =========================================================
         * ALLOWED HTTP METHODS
         * =========================================================
         */
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        /*
         * =========================================================
         * ALLOWED REQUEST HEADERS
         * =========================================================
         */
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With"
                )
        );

        /*
         * =========================================================
         * HEADERS FRONTEND CAN ACCESS
         * =========================================================
         */
        configuration.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Disposition"
                )
        );

        /*
         * =========================================================
         * ALLOW CREDENTIALS
         * =========================================================
         *
         * Useful if you later use cookies/session credentials.
         */
        configuration.setAllowCredentials(
                true
        );

        /*
         * Cache preflight request for 1 hour.
         */
        configuration.setMaxAge(
                3600L
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        /*
         * Apply CORS to every backend endpoint.
         */
        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}