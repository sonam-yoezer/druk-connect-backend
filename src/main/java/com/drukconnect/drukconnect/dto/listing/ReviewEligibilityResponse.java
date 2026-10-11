package com.drukconnect.drukconnect.dto.listing;
import java.util.UUID;
public record ReviewEligibilityResponse(UUID listingId, boolean canReview, boolean whatsappContacted,
    boolean invited, boolean reviewAlreadyExists, String message) {}
