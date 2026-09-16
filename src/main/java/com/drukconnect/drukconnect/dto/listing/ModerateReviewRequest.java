package com.drukconnect.drukconnect.dto.listing;

import com.drukconnect.drukconnect.enums.listing.ReviewModerationAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerateReviewRequest(

        @NotNull
        ReviewModerationAction action,

        @Size(
                max = 500,
                message = "Reason must not exceed 500 characters"
        )
        String reason

) {
}