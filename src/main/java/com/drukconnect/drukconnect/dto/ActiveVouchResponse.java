package com.drukconnect.drukconnect.dto;

import java.time.Instant;
import java.util.UUID;

public record ActiveVouchResponse(

        UUID vouchId,

        UUID voucherUserId,

        String voucherFirstName,

        String voucherLastName,

        String voucherEmail,

        String status,

        Instant vouchedAt

) {
}