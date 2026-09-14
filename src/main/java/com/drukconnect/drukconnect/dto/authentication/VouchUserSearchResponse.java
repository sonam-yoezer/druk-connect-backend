package com.drukconnect.drukconnect.dto.authentication;

import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;

import java.util.UUID;

public record VouchUserSearchResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        AccessTypeEnum accessType
) {}