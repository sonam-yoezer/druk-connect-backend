package com.drukconnect.drukconnect.dto;

import java.util.UUID;

public record SignupResponse(
        UUID userId,
        String status,
        boolean phoneOtpRequired,
        boolean emailOtpRequired,
        String message
) {}
