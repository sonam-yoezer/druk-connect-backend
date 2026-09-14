package com.drukconnect.drukconnect.dto.listing;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ListingReviewResponse(

        UUID reviewId,

        UUID reviewerUserId,

        String reviewer,

        Integer rating,

        Set<String> tags,

        Instant submittedAt

) {
}