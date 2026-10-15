package com.drukconnect.drukconnect.dto.feedback;

import java.time.Instant;
import java.util.UUID;

public record FeedbackSubmissionResponse(

        UUID feedbackId,

        String source,

        String type,

        String status,

        Instant submittedAt,

        String message

) {
}