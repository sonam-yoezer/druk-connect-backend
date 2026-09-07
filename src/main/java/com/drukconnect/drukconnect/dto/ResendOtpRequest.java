package com.drukconnect.drukconnect.dto;

import com.drukconnect.drukconnect.enums.OtpChannel;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResendOtpRequest(@NotNull UUID userId, @NotNull OtpChannel channel) {}
