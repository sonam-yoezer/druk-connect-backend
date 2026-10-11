package com.drukconnect.drukconnect.dto.listing;
import java.time.Instant;
import java.util.UUID;
public record ServiceAvailmentResponse(UUID availmentId, UUID listingId, String listingTitle,
    String listingStatus, UUID buyerUserId, String buyerName, UUID listerUserId, String listerName,
    Instant availedAt, boolean alreadyReviewed, String message) {}
