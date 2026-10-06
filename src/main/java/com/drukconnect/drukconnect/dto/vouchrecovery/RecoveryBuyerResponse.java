package com.drukconnect.drukconnect.dto.vouchrecovery;

import java.util.UUID;

public record RecoveryBuyerResponse(

        UUID userId,
        String firstName,
        String lastName,
        String email,
        String accessType
) {
}