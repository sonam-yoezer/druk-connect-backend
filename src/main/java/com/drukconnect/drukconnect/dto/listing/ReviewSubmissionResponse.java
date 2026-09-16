package com.drukconnect.drukconnect.dto.listing;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record ReviewSubmissionResponse(

        UUID reviewId,

        UUID listingId,

        Integer rating,

        String comment,

        Set<String> tags,

        String status,

        Instant submittedAt,

        String message

) {
}