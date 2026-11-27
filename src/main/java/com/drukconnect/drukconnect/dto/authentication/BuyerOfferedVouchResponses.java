package com.drukconnect.drukconnect.dto.authentication;

import java.time.Instant;
import java.util.UUID;

public final class BuyerOfferedVouchResponses {
    private BuyerOfferedVouchResponses() {}
    public record Candidate(UUID userId, String name, String profileImageUrl, String location,
            String email, long activeVouches, boolean alreadyVouched, boolean requestPending) {}
    public record Result(UUID vouchId, UUID listerUserId, String listerName, Instant vouchedAt,
            boolean alreadyVouched, String message) {}
}
