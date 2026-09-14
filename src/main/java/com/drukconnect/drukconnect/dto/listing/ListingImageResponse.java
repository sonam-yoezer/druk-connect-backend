package com.drukconnect.drukconnect.dto.listing;

import java.util.UUID;

public record ListingImageResponse(

        UUID imageId,

        String imageUrl,

        Integer sortOrder

) {
}