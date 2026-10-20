package com.drukconnect.drukconnect.dto.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

public record ListerDashboardResponse(
        long totalListings,
        long activeListings,
        long inactiveListings,
        long totalListingViews,
        BigDecimal averageRating,
        long approvedReviewCount,
        long activeVouchCount,
        long servicesAvailedCount,
        long uniqueBuyersServed,
        long servicesAvailedLast30Days,
        Instant generatedAt
) {}
