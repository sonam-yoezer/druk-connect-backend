package com.drukconnect.drukconnect.security;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.entity.authentication.User;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class VouchRecoveryTokenService {

    /*
     * =========================================================
     * RECOVERY TOKEN TTL
     * =========================================================
     *
     * Recovery tokens are intentionally short-lived.
     *
     * They are NOT normal access tokens.
     * They are only for:
     *
     * /api/v1/vouch-recovery/**
     *
     * =========================================================
     */
    private static final Duration RECOVERY_TTL =
            Duration.ofMinutes(
                    30
            );


    private final JwtEncoder jwtEncoder;

    private final AppProperties properties;


    public VouchRecoveryTokenService(

            JwtEncoder jwtEncoder,

            AppProperties properties

    ) {

        this.jwtEncoder =
                jwtEncoder;

        this.properties =
                properties;
    }


    /*
     * =========================================================
     * CREATE RECOVERY TOKEN
     * =========================================================
     */
    public String createRecoveryToken(
            User user
    ) {

        Instant now =
                Instant.now();


        Instant expiresAt =
                now.plus(
                        RECOVERY_TTL
                );


        /*
         * =====================================================
         * CLAIMS
         * =====================================================
         *
         * NO token_type claim.
         *
         * The dedicated scope tells the recovery security
         * chain that this token is intended for recovery.
         *
         * =====================================================
         */
        JwtClaimsSet claims =
                JwtClaimsSet.builder()

                        /*
                         * Same application issuer.
                         */
                        .issuer(
                                properties
                                        .jwt()
                                        .issuer()
                        )


                        /*
                         * Authenticated user ID.
                         */
                        .subject(
                                user.getId()
                                        .toString()
                        )


                        /*
                         * Unique token ID.
                         */
                        .id(
                                UUID.randomUUID()
                                        .toString()
                        )


                        .issuedAt(
                                now
                        )


                        .expiresAt(
                                expiresAt
                        )


                        /*
                         * Useful for recovery service validation.
                         */
                        .claim(
                                "access_type",
                                user.getAccessType()
                                        .name()
                        )


                        /*
                         * THIS identifies the token as a
                         * recovery-capable token.
                         *
                         * We are NOT using token_type.
                         */
                        .claim(
                                "scope",
                                List.of(
                                        "VOUCH_RECOVERY"
                                )
                        )


                        .build();


        /*
         * =====================================================
         * JWT HEADER
         * =====================================================
         */
        JwsHeader header =
                JwsHeader
                        .with(
                                MacAlgorithm.HS256
                        )
                        .build();


        /*
         * =====================================================
         * ENCODE TOKEN
         * =====================================================
         */
        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }


    /*
     * =========================================================
     * TTL IN SECONDS
     * =========================================================
     */
    public long expiresInSeconds() {

        return RECOVERY_TTL
                .toSeconds();
    }
}