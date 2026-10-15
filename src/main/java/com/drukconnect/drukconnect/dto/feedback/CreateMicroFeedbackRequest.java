package com.drukconnect.drukconnect.dto.feedback;

import com.drukconnect.drukconnect.enums.feedback.MicroFeedbackRating;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateMicroFeedbackRequest(

        @NotNull
        MicroFeedbackRating rating,

        @Size(max = 2000)
        String comment

) {
}