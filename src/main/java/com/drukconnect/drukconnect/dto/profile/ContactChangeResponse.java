package com.drukconnect.drukconnect.dto.profile;
import java.time.Instant;
import java.util.UUID;
public record ContactChangeResponse(UUID challengeId, String maskedDestination, Instant expiresAt, long resendAfterSeconds, String message) {}
