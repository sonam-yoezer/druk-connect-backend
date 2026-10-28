package com.drukconnect.drukconnect.dto.listing;
import java.time.Instant;
import java.util.UUID;
public record ReviewInvitationResponse(UUID requestId, UUID listingId, String listingTitle,
    UUID buyerUserId, String buyerName, String status, Instant requestedAt, Instant expiresAt,
    String reviewUrl, String loginUrl, String message,
        String buyerProfileImageUrl, String listerProfileImageUrl
) {}
