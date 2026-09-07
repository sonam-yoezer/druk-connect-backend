package com.drukconnect.drukconnect.dto;

import java.time.Instant;
import java.util.UUID;

public record VouchRequestResponse(
        UUID requestId,
        UUID requesterUserId,
        UUID targetUserId,
        String status,
        Instant requestedAt,
        String message
) {}
