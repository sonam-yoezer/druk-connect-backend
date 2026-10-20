package com.drukconnect.drukconnect.service.dashboard;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.dashboard.ListerDashboardResponse;
import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import com.drukconnect.drukconnect.enums.listing.ListingStatus;
import com.drukconnect.drukconnect.enums.listing.ReviewStatus;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.authentication.VouchRepository;
import com.drukconnect.drukconnect.repository.listing.ListingRepository;
import com.drukconnect.drukconnect.repository.listing.ListingReviewRepository;
import com.drukconnect.drukconnect.repository.listing.ListingServiceAvailmentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ListerDashboardService {
    private final UserRepository users;
    private final ListingRepository listings;
    private final ListingReviewRepository reviews;
    private final ListingServiceAvailmentRepository availments;
    private final VouchRepository vouches;

    public ListerDashboardService(UserRepository users, ListingRepository listings,
            ListingReviewRepository reviews, ListingServiceAvailmentRepository availments,
            VouchRepository vouches) {
        this.users = users;
        this.listings = listings;
        this.reviews = reviews;
        this.availments = availments;
        this.vouches = vouches;
    }

    @Transactional(readOnly = true)
    public ListerDashboardResponse getDashboard(UUID userId) {
        var user = users.findById(userId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getAccessType() != AccessTypeEnum.LISTER || user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACTIVE_LISTER_REQUIRED",
                    "An active Lister account is required to view this dashboard");
        }

        Instant generatedAt = Instant.now();
        var listingStats = listings.getDashboardStats(userId, ListingStatus.ACTIVE,
                ListingStatus.INACTIVE, ListingStatus.DELETED);
        var reviewStats = reviews.getDashboardStats(userId, ListingStatus.DELETED, ReviewStatus.APPROVED);
        var serviceStats = availments.getDashboardStats(userId, ListingStatus.DELETED,
                generatedAt.minus(30, ChronoUnit.DAYS), generatedAt);
        BigDecimal rating = reviewStats.getAverageRating() == null ? null
                : BigDecimal.valueOf(reviewStats.getAverageRating()).setScale(1, RoundingMode.HALF_UP);

        return new ListerDashboardResponse(
                listingStats.getTotalListings(), listingStats.getActiveListings(),
                listingStats.getInactiveListings(), listingStats.getTotalListingViews(),
                rating, reviewStats.getApprovedReviewCount(), vouches.countActiveVouchesByUserId(userId),
                serviceStats.getServicesAvailedCount(), serviceStats.getUniqueBuyersServed(),
                serviceStats.getServicesAvailedLast30Days(), generatedAt);
    }
}
