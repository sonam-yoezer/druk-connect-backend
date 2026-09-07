package com.drukconnect.drukconnect.dto;

import com.drukconnect.drukconnect.enums.AccessTypeEnum;

import java.util.UUID;

public record VouchUserSearchResponse(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        AccessTypeEnum accessType
) {}