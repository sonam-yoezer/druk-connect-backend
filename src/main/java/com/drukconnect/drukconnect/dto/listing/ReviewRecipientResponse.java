package com.drukconnect.drukconnect.dto.listing;
import java.util.UUID;
public record ReviewRecipientResponse(UUID userId, String firstName, String lastName, String email,
    String phoneNumber, boolean whatsappContacted, boolean requestPending, boolean serviceAvailed, boolean alreadyReviewed,
        String profileImageUrl
) {}
