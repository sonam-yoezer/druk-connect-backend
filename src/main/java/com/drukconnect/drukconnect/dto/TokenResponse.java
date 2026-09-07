package com.drukconnect.drukconnect.dto;

import java.time.Instant;

public record TokenResponse(
        String tokenType,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        long refreshTokenExpiresIn,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        UserSummary user
) {}
