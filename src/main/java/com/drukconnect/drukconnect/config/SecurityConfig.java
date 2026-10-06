package com.drukconnect.drukconnect.config;

import com.drukconnect.drukconnect.security.AccessTokenValidator;
import com.drukconnect.drukconnect.security.RestAccessDeniedHandler;
import com.drukconnect.drukconnect.security.RestAuthenticationEntryPoint;
import com.drukconnect.drukconnect.security.VouchRecoveryScopeValidator;

import org.springframework.beans.factory.annotation.Qualifier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import org.springframework.core.annotation.Order;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

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
     * JWT SECRET KEY
     * =========================================================
     */
    @Bean
    SecretKey jwtSecretKey(
            AppProperties properties
    ) {

        byte[] secret =
                Base64.getDecoder()
                        .decode(
                                properties
                                        .jwt()
                                        .secretBase64()
                        );


        if (
                secret.length < 32
        ) {

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
     *
     * Used for:
     *
     * - normal access token
     * - vouch recovery token
     *
     * NO token_type claim/check is required.
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
     * NORMAL ACCESS TOKEN DECODER
     * =========================================================
     *
     * Used by normal authenticated APIs.
     *
     * AccessTokenValidator checks:
     *
     * - session
     * - sid
     * - revocation
     * - current access-token validity
     * =========================================================
     */
    @Bean
    @Primary
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
                                        properties
                                                .jwt()
                                                .issuer()
                                ),

                        accessTokenValidator
                )
        );


        return decoder;
    }


    /*
     * =========================================================
     * VOUCH RECOVERY TOKEN DECODER
     * =========================================================
     *
     * Only accepts tokens containing:
     *
     * scope = ["VOUCH_RECOVERY"]
     *
     * Recovery token does NOT need an AuthSession.
     * =========================================================
     */
    @Bean("vouchRecoveryJwtDecoder")
    JwtDecoder vouchRecoveryJwtDecoder(

            SecretKey jwtSecretKey,

            AppProperties properties

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
                                        properties
                                                .jwt()
                                                .issuer()
                                ),

                        new VouchRecoveryScopeValidator()
                )
        );


        return decoder;
    }


    /*
     * =========================================================
     * JWT ROLE CONVERTER
     * =========================================================
     *
     * JWT:
     *
     * roles = ["ENDUSER"]
     * roles = ["ADMIN"]
     *
     * becomes:
     *
     * ROLE_ENDUSER
     * ROLE_ADMIN
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
     * ORDER 1
     *
     * PUBLIC ENDPOINTS
     * =========================================================
     *
     * NO JWT required.
     *
     * IMPORTANT:
     *
     * Email vouch response is public because the signed
     * response token authenticates the request.
     * =========================================================
     */
    @Bean
    @Order(1)
    SecurityFilterChain publicSecurityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http

                .securityMatcher(

                        /*
                         * =====================================
                         * AUTH
                         * =====================================
                         */
                        "/api/v1/auth/signup",
                        "/api/v1/auth/verify-otp",
                        "/api/v1/auth/resend-otp",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",


                        /*
                         * =====================================
                         * PUBLIC VOUCH SEARCH
                         * =====================================
                         *
                         * GET
                         * /api/v1/vouch-requests/users/{userId}/search
                         *
                         * NO JWT REQUIRED.
                         */
                        "/api/v1/vouch-requests/users/*/search",


                        /*
                         * =====================================
                         * PUBLIC VOUCH COUNT
                         * =====================================
                         *
                         * GET
                         * /api/v1/vouch-requests/users/{userId}/vouch-count
                         *
                         * NO JWT REQUIRED.
                         */
                        "/api/v1/vouch-requests/users/*/vouch-count",

                        "/api/v1/vouch-requests/users/*/requests",

                        "/api/v1/vouch-requests/users/*/invitations",


                        /*
                         * =====================================
                         * EMAIL ACCEPT / DECLINE
                         * =====================================
                         *
                         * Uses signed email token instead of JWT.
                         */
                        "/api/v1/vouch-requests/email/**",


                        /*
                         * =====================================
                         * SWAGGER
                         * =====================================
                         */
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",


                        /*
                         * =====================================
                         * ERROR
                         * =====================================
                         */
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
     * ORDER 2
     *
     * VOUCH RECOVERY ENDPOINTS
     * =========================================================
     *
     * Requires:
     *
     * Authorization:
     * Bearer <vouchRecoveryToken>
     *
     * Applies ONLY to:
     *
     * /api/v1/vouch-recovery/**
     *
     * Examples:
     *
     * GET
     * /api/v1/vouch-recovery/buyers/search
     *
     * POST
     * /api/v1/vouch-recovery/requests
     *
     * POST
     * /api/v1/vouch-recovery/invitations
     *
     * =========================================================
     */
    @Bean
    @Order(2)
    SecurityFilterChain vouchRecoverySecurityFilterChain(

            HttpSecurity http,

            @Qualifier("vouchRecoveryJwtDecoder")
            JwtDecoder recoveryJwtDecoder,

            RestAuthenticationEntryPoint authenticationEntryPoint,

            RestAccessDeniedHandler accessDeniedHandler

    ) throws Exception {

        http

                /*
                 * Only recovery URLs enter this chain.
                 */
                .securityMatcher(
                        "/api/v1/vouch-recovery/**"
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
                 * Every recovery endpoint requires
                 * a valid recovery JWT.
                 */
                .authorizeHttpRequests(
                        auth ->
                                auth.anyRequest()
                                        .authenticated()
                )


                /*
                 * IMPORTANT:
                 *
                 * Recovery JWT uses its own decoder.
                 */
                .oauth2ResourceServer(
                        oauth ->
                                oauth

                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )

                                        .jwt(
                                                jwt ->
                                                        jwt.decoder(
                                                                recoveryJwtDecoder
                                                        )
                                        )
                );


        return http.build();
    }


    /*
     * =========================================================
     * ORDER 3
     *
     * NORMAL APPLICATION SECURITY
     * =========================================================
     *
     * Final catch-all chain.
     *
     * Normal access token required unless explicitly
     * marked permitAll below.
     * =========================================================
     */
    @Bean
    @Order(3)
    SecurityFilterChain protectedSecurityFilterChain(

            HttpSecurity http,

            @Qualifier("jwtDecoder")
            JwtDecoder accessJwtDecoder,

            JwtAuthenticationConverter converter,

            RestAuthenticationEntryPoint authenticationEntryPoint,

            RestAccessDeniedHandler accessDeniedHandler

    ) throws Exception {

        http

                /*
                 * IMPORTANT:
                 *
                 * NO securityMatcher here.
                 *
                 * This is intentionally the final
                 * catch-all security chain.
                 */


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


                .authorizeHttpRequests(
                        auth -> auth


                                /*
                                 * =================================
                                 * LISTING OWNER ENDPOINT
                                 * =================================
                                 *
                                 * Must come BEFORE:
                                 *
                                 * /api/v1/listings/**
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/v1/listings/me"
                                )
                                .authenticated()


                                /*
                                 * =================================
                                 * PUBLIC LISTING READS
                                 * =================================
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,

                                        "/api/v1/listings",

                                        "/api/v1/listings/**"
                                )
                                .permitAll()


                                /*
                                 * =================================
                                 * ADMIN
                                 * =================================
                                 */
                                .requestMatchers(
                                        "/api/v1/admin/**"
                                )
                                .hasRole(
                                        "ADMIN"
                                )


                                /*
                                 * =================================
                                 * NORMAL VOUCH APIs
                                 * =================================
                                 *
                                 * NORMAL accessToken required.
                                 *
                                 * Examples:
                                 *
                                 * /me/incoming
                                 *
                                 * /me/given-vouches
                                 *
                                 * /vouches/{id}/withdraw
                                 *
                                 * Old /users/{id}/... endpoints
                                 * also require normal JWT.
                                 *
                                 * NOTE:
                                 *
                                 * /email/** does NOT reach this
                                 * chain because ORDER 1 already
                                 * handles it.
                                 */
                                .requestMatchers(
                                        "/api/v1/vouch-requests/**"
                                )
                                .authenticated()


                                /*
                                 * =================================
                                 * EVERYTHING ELSE
                                 * =================================
                                 *
                                 * Normal JWT required.
                                 */
                                .anyRequest()
                                .authenticated()
                )


                /*
                 * =============================================
                 * NORMAL ACCESS JWT
                 * =============================================
                 */
                .oauth2ResourceServer(
                        oauth ->
                                oauth

                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )

                                        .jwt(
                                                jwt -> jwt

                                                        .decoder(
                                                                accessJwtDecoder
                                                        )

                                                        .jwtAuthenticationConverter(
                                                                converter
                                                        )
                                        )
                );


        return http.build();
    }
}