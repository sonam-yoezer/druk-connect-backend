package com.drukconnect.drukconnect.dto.authentication;

import java.time.Instant;
import java.util.UUID;

public record VouchWithdrawalModerationResponse(

        UUID withdrawalRequestId,

        UUID vouchId,

        String status,

        String adminReason,

        Instant moderatedAt,

        String message

) {
}