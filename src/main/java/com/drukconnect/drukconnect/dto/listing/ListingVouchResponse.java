package com.drukconnect.drukconnect.dto.listing;

import java.time.Instant;
import java.util.UUID;

public record ListingVouchResponse(

        UUID vouchId,

        UUID voucherUserId,

        String voucherName,

        Instant vouchedAt

) {
}