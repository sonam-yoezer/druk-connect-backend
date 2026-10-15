package com.drukconnect.drukconnect.dto.feedback;

import com.drukconnect.drukconnect.enums.feedback.FeedbackPerspective;
import com.drukconnect.drukconnect.enums.feedback.FeedbackType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreatePlatformFeedbackRequest(

        @NotNull
        FeedbackType type,

        @NotNull
        FeedbackPerspective perspective,

        @NotBlank
        @Size(max = 5000)
        String message,

        boolean anonymous

) {
}