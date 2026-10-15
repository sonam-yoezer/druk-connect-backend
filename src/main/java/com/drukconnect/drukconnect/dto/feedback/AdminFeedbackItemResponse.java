package com.drukconnect.drukconnect.dto.feedback;

import java.time.Instant;
import java.util.UUID;

public record AdminFeedbackItemResponse(

        UUID feedbackId,

        String source,

        String type,

        String perspective,

        String microRating,

        String message,

        boolean anonymous,

        UUID submittedByUserId,

        String submitterName,

        String submitterEmail,

        String submitterAccessType,

        boolean canFollowUp,

        String screenshotPath,

        String status,

        String themeKey,

        String themeLabel,

        Instant submittedAt,

        Instant updatedAt,

        Instant resolvedAt

) {
}