package com.drukconnect.drukconnect.dto.vouchrecovery;

import java.time.Instant;
import java.util.UUID;

public record RecoveryVouchInvitationResponse(

        UUID invitationId,

        String email,

        String status,

        Instant createdAt,

        String message

) {
}