package com.drukconnect.drukconnect.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record ListerScamReportResponse(

        UUID reportId,

        UUID reportedListerUserId,

        String reportedListerName,

        UUID reporterUserId,

        UUID sourceListingId,

        String sourceListingTitle,

        String reason,

        String details,

        String status,

        Instant reportedAt,

        Instant moderatedAt,

        String moderationReason,

        String message

) {
}