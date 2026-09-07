package com.drukconnect.drukconnect.dto;

import java.time.Instant;
import java.util.UUID;

public record VouchReceivedResponse(
        UUID vouchId,
        UUID voucherUserId,
        String firstName,
        String lastName,
        String email,
        String status,
        Instant vouchedAt
) {}