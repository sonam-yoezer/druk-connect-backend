package com.drukconnect.drukconnect.dto.listing;

import com.drukconnect.drukconnect.enums.listing.ListingAvailability;
import com.drukconnect.drukconnect.enums.listing.ListingPricingType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record ListingDetailResponse(

        UUID id,

        String listingTitle,

        String city,

        String description,

        String cuisine,

        String serviceType,

        Integer minimumOrder,

        Integer serves,

        Set<String> dietaryOptions,

        ListingAvailability availability,

        ListingPricingType pricingType,

        BigDecimal rateAmount,

        String currencyCode,

        long views,

        List<ListingImageResponse> images,

        long totalReviewer,

        BigDecimal averageRating,

        int averageRatingStar,

        ListingOwnerResponse lister,

        List<ListingReviewResponse> reviews,

        Instant createdAt

) {
}