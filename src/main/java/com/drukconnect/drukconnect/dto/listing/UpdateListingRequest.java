package com.drukconnect.drukconnect.dto.listing;

import com.drukconnect.drukconnect.enums.listing.ListingAvailability;
import com.drukconnect.drukconnect.enums.listing.ListingPricingType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateListingRequest(

        ListingPricingType pricingType,

        @DecimalMin(
                value = "0.01",
                message = "Rate amount must be greater than zero"
        )
        BigDecimal rateAmount,

        ListingAvailability availability

) {
}
