package com.drukconnect.drukconnect.service.moderation;

import com.drukconnect.drukconnect.common.ApiException;

import com.drukconnect.drukconnect.repository.moderation
        .ListerPostingRestrictionRepository;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ListerRestrictionService {

    private final ListerPostingRestrictionRepository
            restrictionRepository;


    public ListerRestrictionService(
            ListerPostingRestrictionRepository restrictionRepository
    ) {

        this.restrictionRepository =
                restrictionRepository;
    }


    @Transactional(readOnly = true)
    public boolean isRestricted(
            UUID userId
    ) {

        return restrictionRepository
                .existsByListerIdAndActiveTrue(
                        userId
                );
    }


    /*
     * Call this BEFORE creating any new listing.
     */
    @Transactional(readOnly = true)
    public void assertCanCreateListing(
            UUID userId
    ) {

        if (
                restrictionRepository
                        .existsByListerIdAndActiveTrue(
                                userId
                        )
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_POSTING_RESTRICTED",
                    "Your account is currently restricted from creating new listings. "
                            + "You may submit an appeal."
            );
        }
    }
}