package com.drukconnect.drukconnect.dto.listing;

import com.drukconnect.drukconnect.enums.listing.ListingAvailability;
import com.drukconnect.drukconnect.enums.listing.ListingPricingType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ListingSummaryResponse(

        UUID id,

        String listingTitle,

        String city,

        String cuisine,

        String serviceType,

        ListingAvailability availability,

        ListingPricingType pricingType,

        BigDecimal rateAmount,

        String currencyCode,

        long views,

        List<ListingImageResponse> images,

        long totalReviewer,

        BigDecimal averageRating,

        int averageRatingStar,

        String listerName

) {
}