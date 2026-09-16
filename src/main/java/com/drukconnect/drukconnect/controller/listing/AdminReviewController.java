package com.drukconnect.drukconnect.controller.listing;

import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.service.listing.ListingReviewService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(
        "/api/v1/admin/reviews"
)
public class AdminReviewController {

    private final ListingReviewService reviewService;

    public AdminReviewController(
            ListingReviewService reviewService
    ) {
        this.reviewService =
                reviewService;
    }

    @PostMapping(
            "/{reviewId}/approveOrReject"
    )
    public ReviewModerationResponse moderateReview(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID reviewId,

            @Valid
            @RequestBody
            ModerateReviewRequest request,

            HttpServletRequest httpRequest
    ) {

        UUID adminId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return reviewService
                .moderateReview(
                        adminId,
                        reviewId,
                        request,
                        RequestMetadata.from(
                                httpRequest
                        )
                );
    }

    @GetMapping
    public PagedAdminReviewResponse getPendingReviews(

            @RequestParam(
                    defaultValue = "1"
            )
            int page,

            @RequestParam(
                    defaultValue = "10"
            )
            int size
    ) {

        return reviewService
                .getPendingReviews(
                        page,
                        size
                );
    }
}