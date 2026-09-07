package com.drukconnect.drukconnect.dto;

import java.util.UUID;

public record VouchCountResponse(

        UUID userId,

        long activeVouches,

        long requiredVouches,

        long remainingVouches,

        boolean requirementMet

) {
}