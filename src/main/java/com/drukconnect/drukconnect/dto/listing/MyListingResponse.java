package com.drukconnect.drukconnect.dto.listing;

import com.drukconnect.drukconnect.enums.listing.ListingAvailability;
import com.drukconnect.drukconnect.enums.listing.ListingPricingType;
import com.drukconnect.drukconnect.enums.listing.ListingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record MyListingResponse(

        UUID id,

        String listingTitle,

        String listingCategory,

        String city,

        String cuisine,

        String serviceType,

        ListingAvailability availability,

        ListingPricingType pricingType,

        BigDecimal rateAmount,

        String currencyCode,

        Long views,

        ListingStatus status,

        List<ListingImageResponse> images,

        long totalReviewer,

        BigDecimal averageRating,

        int averageRatingStar,

        Instant createdAt,

        Instant updatedAt

) {
}