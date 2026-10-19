package com.drukconnect.drukconnect.dto.listing;

import com.drukconnect.drukconnect.enums.listing.ListingPricingType;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

public record UpdateListingRequest(

        ListingPricingType pricingType,

        @DecimalMin(
                value = "0.01",
                message = "Rate amount must be greater than zero"
        )
        BigDecimal rateAmount,

        String availability,

        @Size(max = 5) List<@NotNull UUID> retainedImageIds

) {
}
