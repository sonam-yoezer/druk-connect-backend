package com.drukconnect.drukconnect.dto;

import java.util.List;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        List<String> roles
) {}
