package com.drukconnect.drukconnect.controller.listing;

import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.listing.ListingReviewResponse;
import com.drukconnect.drukconnect.dto.listing.PagedAdminReviewResponse;
import com.drukconnect.drukconnect.dto.listing.RejectReviewRequest;
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

    @PatchMapping(
            "/{reviewId}/approve"
    )
    public ListingReviewResponse approve(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID reviewId,

            HttpServletRequest request
    ) {

        UUID adminId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return reviewService.approve(
                adminId,
                reviewId,
                RequestMetadata.from(
                        request
                )
        );
    }

    @PatchMapping(
            "/{reviewId}/reject"
    )
    public ResponseEntity<Void> reject(

            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID reviewId,

            @Valid
            @RequestBody
            RejectReviewRequest body,

            HttpServletRequest request
    ) {

        UUID adminId =
                UUID.fromString(
                        jwt.getSubject()
                );

        reviewService.reject(
                adminId,
                reviewId,
                body.reason(),
                RequestMetadata.from(
                        request
                )
        );

        return ResponseEntity
                .noContent()
                .build();
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