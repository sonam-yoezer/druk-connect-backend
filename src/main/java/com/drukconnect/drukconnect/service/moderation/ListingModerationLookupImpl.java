package com.drukconnect.drukconnect.service.moderation;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.repository.listing.ListingRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListingModerationLookupImpl
        implements ListingModerationLookup {

    private final ListingRepository listingRepository;


    public ListingModerationLookupImpl(
            ListingRepository listingRepository
    ) {

        this.listingRepository =
                listingRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public ListingTarget requireListing(
            UUID listingId
    ) {

        /*
         * =========================================================
         * FIND LISTING
         * =========================================================
         */
        Listing listing =
                listingRepository
                        .findById(
                                listingId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "LISTING_NOT_FOUND",
                                        "Listing was not found"
                                )
                        );


        /*
         * =========================================================
         * VALIDATE LISTER
         * =========================================================
         */
        if (
                listing.getLister()
                        == null
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "LISTING_OWNER_NOT_FOUND",
                    "The listing does not have a valid Lister"
            );
        }


        /*
         * =========================================================
         * RETURN MODERATION TARGET
         * =========================================================
         *
         * listingId
         *      = listing used as evidence
         *
         * ownerUserId
         *      = actual LISTER being reported
         *
         * title
         *      = snapshot of listing title
         */
        return new ListingTarget(

                listing.getId(),

                listing.getLister()
                        .getId(),

                listing.getListingTitle()
        );
    }
}