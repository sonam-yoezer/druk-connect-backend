package com.drukconnect.drukconnect.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;
public record CreateVouchRequest(
        @NotNull UUID targetUserId,
        @NotBlank @Size(max=500) String message
) {}
