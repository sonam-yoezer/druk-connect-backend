package com.drukconnect.drukconnect.dto.moderation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateListerScamReportRequest(

        @NotBlank
        @Size(max = 500)
        String reason,

        @Size(max = 2000)
        String details

) {
}