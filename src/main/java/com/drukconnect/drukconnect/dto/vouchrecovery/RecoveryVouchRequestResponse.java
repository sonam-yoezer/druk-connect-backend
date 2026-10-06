package com.drukconnect.drukconnect.dto.vouchrecovery;

import java.time.Instant;
import java.util.UUID;

public record RecoveryVouchRequestResponse(

        UUID requestId,

        UUID listerUserId,

        UUID buyerUserId,

        String buyerName,

        String buyerEmail,

        String status,

        Instant requestedAt,

        String message

) {
}