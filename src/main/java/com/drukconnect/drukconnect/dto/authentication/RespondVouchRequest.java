package com.drukconnect.drukconnect.dto.authentication;

import jakarta.validation.constraints.NotNull;

public record RespondVouchRequest(@NotNull Boolean accept) {}
