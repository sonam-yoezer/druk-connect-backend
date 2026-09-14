package com.drukconnect.drukconnect.dto.listing;

import jakarta.validation.constraints.Size;

public record RejectReviewRequest(

        @Size(max = 500)
        String reason

) {
}