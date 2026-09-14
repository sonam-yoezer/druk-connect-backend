package com.drukconnect.drukconnect.dto.authentication;

public record VerifyOtpResponse(
        boolean verified,
        boolean phoneVerified,
        boolean emailVerified,
        boolean accountActive,
        String message
) {}
