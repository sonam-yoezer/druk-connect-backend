package com.drukconnect.drukconnect.dto.dashboard;

import java.time.Instant;

public record BuyerDashboardResponse(
        long servicesAvailedCount,
        long uniqueProvidersUsed,
        long servicesAvailedLast30Days,
        long submittedReviewCount,
        long approvedReviewCount,
        long pendingReviewCount,
        long rejectedReviewCount,
        long pendingReviewInvitationCount,
        long pendingVouchRequestsReceived,
        long activeVouchesGiven,
        Instant generatedAt
) {}
