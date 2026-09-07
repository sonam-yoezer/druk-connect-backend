package com.drukconnect.drukconnect.dto;

import java.time.Instant;
import java.util.UUID;

public record VouchInvitationResponse(
        UUID invitationId,
        String email,
        String status,
        Instant expiresAt,
        String message
) {}