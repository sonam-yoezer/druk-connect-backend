package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.listing.*;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.*;

import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.listing.*;

import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.repository.authentication.*;


import com.drukconnect.drukconnect.service.authentication.AuditService;
import org.springframework.data.domain.*;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ListingReviewService {

    private final ListingRepository listingRepository;

    private final ListingReviewRepository reviewRepository;

    private final UserRepository userRepository;

    private final AuditService auditService;

    public ListingReviewService(
            ListingRepository listingRepository,
            ListingReviewRepository reviewRepository,
            UserRepository userRepository,
            AuditService auditService
    ) {

        this.listingRepository =
                listingRepository;

        this.reviewRepository =
                reviewRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;
    }

    /*
     * =========================================================
     * BUYER SUBMITS REVIEW
     * =========================================================
     */
    @Transactional
    public ReviewSubmissionResponse createReview(
            UUID buyerId,
            UUID listingId,
            CreateListingReviewRequest request,
            RequestMetadata meta
    ) {

        User buyer =
                userRepository
                        .findById(
                                buyerId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User not found"
                                )
                        );

        if (
                buyer.getAccessType()
                        != AccessTypeEnum.BUYER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "BUYER_REQUIRED",
                    "Only Buyer accounts can submit reviews"
            );
        }

        Listing listing =
                listingRepository
                        .findByIdAndStatus(
                                listingId,
                                ListingStatus.ACTIVE
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "LISTING_NOT_FOUND",
                                        "Listing not found"
                                )
                        );

        /*
         * Prevent another PENDING or APPROVED review.
         *
         * REJECTED review may be resubmitted.
         */
        boolean existing =
                reviewRepository
                        .existsByListingIdAndReviewerIdAndStatusIn(
                                listingId,
                                buyerId,
                                List.of(
                                        ReviewStatus.PENDING,
                                        ReviewStatus.APPROVED
                                )
                        );

        if (
                existing
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REVIEW_ALREADY_EXISTS",
                    "You already have a pending or approved review for this listing"
            );
        }

        ListingReview review =
                new ListingReview();

        review.setListing(
                listing
        );

        review.setReviewer(
                buyer
        );

        review.setRating(
                request.rating()
        );

        review.setTags(
                request.tags()
                        == null
                        ? Set.of()
                        : request.tags()
        );

        review.setStatus(
                ReviewStatus.PENDING
        );

        review =
                reviewRepository
                        .saveAndFlush(
                                review
                        );

        auditService.log(
                "LISTING_REVIEW_SUBMITTED",
                buyerId,
                null,
                listing.getLister()
                        .getId(),
                null,
                meta,
                "{\"listingId\":\""
                        + listingId
                        + "\","
                        + "\"reviewId\":\""
                        + review.getId()
                        + "\"}"
        );

        return new ReviewSubmissionResponse(

                review.getId(),

                listingId,

                review.getRating(),

                review.getTags(),

                review.getStatus()
                        .name(),

                review.getSubmittedAt(),

                "Review submitted successfully and is awaiting admin approval."
        );
    }

    /*
     * =========================================================
     * ADMIN APPROVE
     * =========================================================
     */
    @Transactional
    public ListingReviewResponse approve(
            UUID adminId,
            UUID reviewId,
            RequestMetadata meta
    ) {

        User admin =
                userRepository
                        .findById(
                                adminId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "Admin user not found"
                                )
                        );

        ListingReview review =
                reviewRepository
                        .findById(
                                reviewId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "REVIEW_NOT_FOUND",
                                        "Review not found"
                                )
                        );

        if (
                review.getStatus()
                        != ReviewStatus.PENDING
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REVIEW_ALREADY_MODERATED",
                    "This review has already been moderated"
            );
        }

        review.setStatus(
                ReviewStatus.APPROVED
        );

        review.setModeratedAt(
                Instant.now()
        );

        review.setModeratedBy(
                admin
        );

        review =
                reviewRepository
                        .saveAndFlush(
                                review
                        );

        auditService.log(
                "LISTING_REVIEW_APPROVED",
                adminId,
                null,
                review.getReviewer()
                        .getId(),
                null,
                meta,
                "{\"listingId\":\""
                        + review.getListing()
                        .getId()
                        + "\","
                        + "\"reviewId\":\""
                        + reviewId
                        + "\"}"
        );

        return mapApproved(
                review
        );
    }

    /*
     * =========================================================
     * ADMIN REJECT
     * =========================================================
     */
    @Transactional
    public void reject(
            UUID adminId,
            UUID reviewId,
            String reason,
            RequestMetadata meta
    ) {

        User admin =
                userRepository
                        .findById(
                                adminId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "Admin user not found"
                                )
                        );

        ListingReview review =
                reviewRepository
                        .findById(
                                reviewId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "REVIEW_NOT_FOUND",
                                        "Review not found"
                                )
                        );

        if (
                review.getStatus()
                        != ReviewStatus.PENDING
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REVIEW_ALREADY_MODERATED",
                    "This review has already been moderated"
            );
        }

        review.setStatus(
                ReviewStatus.REJECTED
        );

        review.setModeratedAt(
                Instant.now()
        );

        review.setModeratedBy(
                admin
        );

        review.setRejectionReason(
                reason
        );

        reviewRepository
                .saveAndFlush(
                        review
                );

        auditService.log(
                "LISTING_REVIEW_REJECTED",
                adminId,
                null,
                review.getReviewer()
                        .getId(),
                null,
                meta,
                "{\"listingId\":\""
                        + review.getListing()
                        .getId()
                        + "\","
                        + "\"reviewId\":\""
                        + reviewId
                        + "\"}"
        );
    }

    private ListingReviewResponse mapApproved(
            ListingReview review
    ) {

        return new ListingReviewResponse(

                review.getId(),

                review.getReviewer()
                        .getId(),

                (
                        review.getReviewer()
                                .getFirstName()
                                + " "
                                + review.getReviewer()
                                .getLastName()
                ).trim(),

                review.getRating(),

                review.getTags(),

                review.getSubmittedAt()
        );
    }

    @Transactional(readOnly = true)
    public PagedAdminReviewResponse getPendingReviews(
            int page,
            int size
    ) {

        int safePage =
                Math.max(
                        page,
                        1
                );

        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        50
                );

        Page<ListingReview> result =
                reviewRepository
                        .findByStatusOrderBySubmittedAtAsc(
                                ReviewStatus.PENDING,
                                PageRequest.of(
                                        safePage - 1,
                                        safeSize
                                )
                        );

        List<AdminReviewResponse> responses =
                result.getContent()
                        .stream()
                        .map(
                                review -> {

                                    User reviewer =
                                            review.getReviewer();

                                    return new AdminReviewResponse(

                                            review.getId(),

                                            review.getListing()
                                                    .getId(),

                                            review.getListing()
                                                    .getListingTitle(),

                                            reviewer.getId(),

                                            (
                                                    reviewer.getFirstName()
                                                            + " "
                                                            + reviewer.getLastName()
                                            ).trim(),

                                            reviewer.getEmail(),

                                            review.getRating(),

                                            review.getTags(),

                                            review.getStatus()
                                                    .name(),

                                            review.getSubmittedAt()
                                    );
                                }
                        )
                        .toList();

        return new PagedAdminReviewResponse(

                responses,

                result.getNumber() + 1,

                result.getSize(),

                result.getTotalElements(),

                result.getTotalPages()
        );
    }
}
