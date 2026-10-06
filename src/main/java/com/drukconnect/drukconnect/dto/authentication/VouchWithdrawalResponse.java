package com.drukconnect.drukconnect.dto.authentication;

import java.time.Instant;
import java.util.UUID;

public record VouchWithdrawalResponse(

        UUID withdrawalRequestId,

        UUID vouchId,

        UUID buyerUserId,

        String buyerName,

        UUID listerUserId,

        String listerName,

        String reason,

        String status,

        Instant requestedAt,

        String message

) {
}
