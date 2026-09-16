package com.drukconnect.drukconnect.dto.listing;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ReviewModerationResponse(

        UUID reviewId,

        UUID listingId,

        UUID reviewerUserId,

        String reviewer,

        Integer rating,

        String comment,

        Set<String> tags,

        String status,

        String rejectionReason,

        Instant submittedAt,

        Instant moderatedAt,

        String message

) {
}