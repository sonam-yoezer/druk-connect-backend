package com.drukconnect.drukconnect.service.moderation;

import java.util.UUID;

public interface ListingModerationLookup {

    ListingTarget requireListing(
            UUID listingId
    );


    record ListingTarget(

            UUID listingId,

            UUID ownerUserId,

            String title

    ) {
    }
}