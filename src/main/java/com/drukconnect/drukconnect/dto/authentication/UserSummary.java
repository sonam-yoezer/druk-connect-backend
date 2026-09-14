package com.drukconnect.drukconnect.dto.authentication;

import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;

import java.util.List;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        AccessTypeEnum accessType,
        List<String> roles
) {}
