package com.drukconnect.drukconnect.dto.authentication;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(@NotBlank String refreshToken) {}
