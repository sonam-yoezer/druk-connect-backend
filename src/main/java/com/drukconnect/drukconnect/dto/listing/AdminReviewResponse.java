package com.drukconnect.drukconnect.dto.listing;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AdminReviewResponse(

        UUID reviewId,

        UUID listingId,

        String listingTitle,

        UUID reviewerUserId,

        String reviewerName,

        String reviewerEmail,

        Integer rating,

        String comment,

        Set<String> tags,

        String status,

        Instant submittedAt

) {
}