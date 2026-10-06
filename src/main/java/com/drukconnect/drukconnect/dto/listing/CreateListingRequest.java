package com.drukconnect.drukconnect.dto.listing;

import com.drukconnect.drukconnect.enums.listing.ListingAvailability;
import com.drukconnect.drukconnect.enums.listing.ListingPricingType;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.Set;

public record CreateListingRequest(

        @NotBlank
        @Size(max = 180)
        String listingTitle,

        @NotBlank
        @Size(max = 120)
        String listingCategory,

        @NotBlank
        @Size(max = 120)
        String suburb,

        @NotBlank
        @Size(max = 100)
        String state,

        @NotBlank
        @Size(max = 20)
        String postcode,

        @NotBlank
        String description,

        @NotBlank
        @Size(max = 120)
        String serviceType,

        Set<String> dietaryOptions,

        @NotNull
        ListingAvailability availability,

        @NotNull
        ListingPricingType pricingType,

        @DecimalMin("0.01")
        BigDecimal rateAmount

) {
}