package com.drukconnect.drukconnect.controller.listing;

import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.service.listing.ListingReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/listings"
)
public class ListingReviewController {

    private final ListingReviewService reviewService;

    public ListingReviewController(
            ListingReviewService reviewService
    ) {
        this.reviewService =
                reviewService;
    }

    /*
     * BUYER ONLY.
     */
    @PostMapping(
            "/{listingId}/reviews"
    )
    @ResponseStatus(
            HttpStatus.CREATED
    )
    public ReviewSubmissionResponse createReview(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID listingId,

            @Valid
            @RequestBody
            CreateListingReviewRequest request,

            HttpServletRequest httpRequest
    ) {

        UUID buyerId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return reviewService.createReview(
                buyerId,
                listingId,
                request,
                RequestMetadata.from(
                        httpRequest
                )
        );
    }
}