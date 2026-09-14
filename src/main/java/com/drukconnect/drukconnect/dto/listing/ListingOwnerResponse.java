package com.drukconnect.drukconnect.dto.listing;

import java.util.List;
import java.util.UUID;

public record ListingOwnerResponse(

        UUID userId,

        String name,

        String email,

        String emailLink,

        String phoneNumber,

        String whatsappLink,

        long activeVouches,

        List<ListingVouchResponse> vouches

) {
}