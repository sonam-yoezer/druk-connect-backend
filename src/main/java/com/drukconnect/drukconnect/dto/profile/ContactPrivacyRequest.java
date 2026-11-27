package com.drukconnect.drukconnect.dto.profile;

import jakarta.validation.constraints.NotNull;

public record ContactPrivacyRequest(@NotNull Boolean showEmail, @NotNull Boolean showPhoneNumber) {}
