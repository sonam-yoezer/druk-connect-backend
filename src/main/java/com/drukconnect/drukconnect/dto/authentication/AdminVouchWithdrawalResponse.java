package com.drukconnect.drukconnect.dto.authentication;

import java.time.Instant;
import java.util.UUID;

public record AdminVouchWithdrawalResponse(

        UUID withdrawalRequestId,

        UUID vouchId,

        UUID buyerUserId,

        String buyerName,

        String buyerEmail,

        String buyerPhoneNumber,

        UUID listerUserId,

        String listerName,

        String listerEmail,

        String listerPhoneNumber,

        String reason,

        String status,

        Instant vouchedAt,

        Instant requestedAt

) {
}
