package com.drukconnect.drukconnect.dto.authentication;

import com.drukconnect.drukconnect.enums.authentication.OtpChannel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResendOtpRequest(@NotNull UUID userId, @NotNull OtpChannel channel) {}
