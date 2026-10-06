package com.drukconnect.drukconnect.dto.authentication;

import java.time.Instant;
import java.util.UUID;

public record BuyerVouchResponse(

        UUID vouchId,

        UUID listerUserId,

        String listerName,

        String listerEmail,

        String listerPhoneNumber,

        String vouchStatus,

        Instant vouchedAt,

        boolean withdrawalPending

) {
}