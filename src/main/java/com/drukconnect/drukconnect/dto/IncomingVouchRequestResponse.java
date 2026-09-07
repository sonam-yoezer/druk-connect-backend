package com.drukconnect.drukconnect.dto;

import java.time.Instant;
import java.util.UUID;

public record IncomingVouchRequestResponse(
        UUID requestId,
        UUID requesterUserId,
        String requesterFirstName,
        String requesterLastName,
        String requesterEmail,
        String message,
        String status,
        Instant requestedAt,
        Instant respondedAt
) {
}