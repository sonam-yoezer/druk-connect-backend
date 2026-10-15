package com.drukconnect.drukconnect.dto.feedback;

import com.drukconnect.drukconnect.enums.feedback.FeedbackStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateFeedbackStatusRequest(

        @NotNull
        FeedbackStatus status

) {
}