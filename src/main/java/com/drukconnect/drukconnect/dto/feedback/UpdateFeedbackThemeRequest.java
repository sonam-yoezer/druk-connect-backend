package com.drukconnect.drukconnect.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFeedbackThemeRequest(

        @NotBlank
        @Size(max = 100)
        String themeKey,

        @NotBlank
        @Size(max = 300)
        String themeLabel

) {
}