package com.drukconnect.drukconnect.dto;

import com.drukconnect.drukconnect.enums.OtpChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record VerifyOtpRequest(
        @NotNull UUID userId,
        @NotNull OtpChannel channel,
        @NotBlank @Pattern(regexp = "^\\d{4}$", message = "OTP must be exactly 4 digits") String otp
) {}
