package com.drukconnect.drukconnect.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record ListerScamAppealResponse(

        UUID appealId,

        UUID listerUserId,

        UUID restrictionId,

        UUID sourceReportId,

        String status,

        String appealReason,

        Instant submittedAt,

        Instant moderatedAt,

        String moderationReason,

        String message

) {
}