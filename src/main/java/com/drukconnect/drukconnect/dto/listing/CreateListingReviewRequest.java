package com.drukconnect.drukconnect.dto.listing;

import jakarta.validation.constraints.*;

import java.util.Set;

public record CreateListingReviewRequest(

        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @NotBlank
        @Size(max = 2000)
        String comment,

        @Size(max = 5)
        Set<
                @NotBlank
                @Size(max = 80)
                        String
                > tags

) {
}