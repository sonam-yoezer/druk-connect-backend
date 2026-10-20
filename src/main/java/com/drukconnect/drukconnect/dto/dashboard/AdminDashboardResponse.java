package com.drukconnect.drukconnect.dto.dashboard;

import java.time.Instant;

public record AdminDashboardResponse(
        long totalUsers,
        long activeUsers,
        long newUsersLast7Days,
        long totalListings,
        long activeListings,
        long newListingsLast7Days,
        long pendingReviewCount,
        long pendingVouchWithdrawalCount,
        long pendingScamReportCount,
        long pendingAppealCount,
        long unresolvedFeedbackCount,
        Instant generatedAt
) {}
