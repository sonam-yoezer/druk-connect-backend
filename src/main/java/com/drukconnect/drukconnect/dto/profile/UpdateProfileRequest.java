package com.drukconnect.drukconnect.dto.profile;

import jakarta.validation.constraints.*;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 1000) String bio,
        @Size(max = 180) String location) {}
