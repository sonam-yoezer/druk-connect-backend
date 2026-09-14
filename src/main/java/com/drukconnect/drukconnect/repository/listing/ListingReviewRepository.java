package com.drukconnect.drukconnect.repository.listing;


import com.drukconnect.drukconnect.entity.listing.ListingReview;
import com.drukconnect.drukconnect.enums.listing.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ListingReviewRepository
        extends JpaRepository<ListingReview, UUID> {

    long countByListingIdAndStatus(
            UUID listingId,
            ReviewStatus status
    );

    List<ListingReview>
    findByListingIdAndStatusOrderBySubmittedAtDesc(
            UUID listingId,
            ReviewStatus status
    );

    Page<ListingReview>
    findByStatusOrderBySubmittedAtAsc(
            ReviewStatus status,
            Pageable pageable
    );

    boolean
    existsByListingIdAndReviewerIdAndStatusIn(
            UUID listingId,
            UUID reviewerId,
            Collection<ReviewStatus> statuses
    );

    @Query("""
            SELECT AVG(r.rating)
            FROM ListingReview r
            WHERE r.listing.id = :listingId
            AND r.status = :status
            """)
    Double getAverageRating(
            @Param("listingId")
            UUID listingId,

            @Param("status")
            ReviewStatus status
    );
}