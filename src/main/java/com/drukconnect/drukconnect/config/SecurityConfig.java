package com.drukconnect.drukconnect.config;

import com.drukconnect.drukconnect.security.AccessTokenValidator;
import com.drukconnect.drukconnect.security.RestAccessDeniedHandler;
import com.drukconnect.drukconnect.security.RestAuthenticationEntryPoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.security.oauth2.jwt.*;

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;

@Configuration
public class SecurityConfig {

    /*
     * =========================================================
     * PASSWORD ENCODER
     * =========================================================
     */
    @Bean
    PasswordEncoder passwordEncoder() {

        return PasswordEncoderFactories
                .createDelegatingPasswordEncoder();
    }

    /*
     * =========================================================
     * JWT SECRET
     * =========================================================
     */
    @Bean
    SecretKey jwtSecretKey(
            AppProperties properties
    ) {

        byte[] secret =
                Base64.getDecoder()
                        .decode(
                                properties.jwt()
                                        .secretBase64()
                        );

        if (secret.length < 32) {

            throw new IllegalStateException(
                    "JWT_SECRET_BASE64 must decode to at least 32 bytes"
            );
        }

        return new SecretKeySpec(
                secret,
                "HmacSHA256"
        );
    }

    /*
     * =========================================================
     * JWT ENCODER
     * =========================================================
     */
    @Bean
    JwtEncoder jwtEncoder(
            SecretKey jwtSecretKey
    ) {

        return NimbusJwtEncoder
                .withSecretKey(
                        jwtSecretKey
                )
                .algorithm(
                        MacAlgorithm.HS256
                )
                .build();
    }

    /*
     * =========================================================
     * JWT DECODER
     * =========================================================
     */
    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            AppProperties properties,
            AccessTokenValidator accessTokenValidator
    ) {

        NimbusJwtDecoder decoder =
                NimbusJwtDecoder
                        .withSecretKey(
                                jwtSecretKey
                        )
                        .macAlgorithm(
                                MacAlgorithm.HS256
                        )
                        .build();

        decoder.setJwtValidator(

                new DelegatingOAuth2TokenValidator<>(

                        JwtValidators
                                .createDefaultWithIssuer(
                                        properties.jwt()
                                                .issuer()
                                ),

                        accessTokenValidator
                )
        );

        return decoder;
    }

    /*
     * =========================================================
     * JWT ROLE CONVERTER
     * =========================================================
     */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter roles =
                new JwtGrantedAuthoritiesConverter();

        roles.setAuthoritiesClaimName(
                "roles"
        );

        roles.setAuthorityPrefix(
                "ROLE_"
        );

        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                roles
        );

        return converter;
    }

    /*
     * =========================================================
     * PUBLIC SECURITY FILTER CHAIN
     * =========================================================
     *
     * ONLY endpoints that should NEVER attempt JWT
     * authentication belong here.
     *
     * IMPORTANT:
     *
     * Listings are NOT included here.
     *
     * Listing GET endpoints will be made public
     * in the second chain based on HttpMethod.GET.
     *
     * =========================================================
     */
    @Bean
    @Order(1)
    SecurityFilterChain publicSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                .securityMatcher(

                        "/api/v1/auth/signup",

                        "/api/v1/auth/verify-otp",

                        "/api/v1/auth/resend-otp",

                        "/api/v1/auth/login",

                        "/api/v1/auth/refresh",

                        "/swagger-ui/**",

                        "/swagger-ui.html",

                        "/v3/api-docs/**",

                        "/api/v1/vouch-requests/users/**",

                        "/api/v1/vouch-requests/email/**",

                        "/error"
                )

                .csrf(
                        csrf ->
                                csrf.disable()
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .authorizeHttpRequests(
                        auth ->
                                auth.anyRequest()
                                        .permitAll()
                );

        return http.build();
    }

    /*
     * =========================================================
     * PROTECTED / JWT SECURITY FILTER CHAIN
     * =========================================================
     *
     * Listings enter THIS chain.
     *
     * GET listing endpoints:
     *      public
     *
     * POST listing:
     *      authenticated
     *
     * POST review:
     *      authenticated
     *
     * Admin endpoints:
     *      ROLE_ADMIN
     *
     * =========================================================
     */
    @Bean
    @Order(2)
    SecurityFilterChain protectedSecurityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter converter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http

                .csrf(
                        csrf ->
                                csrf.disable()
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                /*
                 * ---------------------------------------------
                 * SECURITY JSON ERRORS
                 * ---------------------------------------------
                 */
                .exceptionHandling(
                        exceptions ->
                                exceptions

                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )

                                        .accessDeniedHandler(
                                                accessDeniedHandler
                                        )
                )

                /*
                 * ---------------------------------------------
                 * AUTHORIZATION
                 * ---------------------------------------------
                 */
                .authorizeHttpRequests(
                        auth -> auth

                                /*
                                 * =====================================
                                 * PUBLIC LISTING READ ENDPOINTS
                                 * =====================================
                                 *
                                 * No Bearer token required.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/listings",
                                        "/api/v1/listings/**"
                                )
                                .permitAll()

                                /*
                                 * =====================================
                                 * ADMIN APIs
                                 * =====================================
                                 */
                                .requestMatchers(
                                        "/api/v1/admin/**"
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                /*
                                 * =====================================
                                 * EVERYTHING ELSE
                                 * =====================================
                                 *
                                 * Includes:
                                 *
                                 * POST /api/v1/listings
                                 *
                                 * POST
                                 * /api/v1/listings/{id}/reviews
                                 *
                                 * logout, normal vouch APIs, etc.
                                 */
                                .anyRequest()
                                .authenticated()
                )

                /*
                 * ---------------------------------------------
                 * JWT RESOURCE SERVER
                 * ---------------------------------------------
                 */
                .oauth2ResourceServer(
                        oauth ->
                                oauth

                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )

                                        .jwt(
                                                jwt ->
                                                        jwt.jwtAuthenticationConverter(
                                                                converter
                                                        )
                                        )
                );

        return http.build();
    }
}