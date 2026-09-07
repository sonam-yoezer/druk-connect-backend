package com.drukconnect.drukconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateVouchInvitationRequest(

        @NotBlank
        @Email
        String email,

        @Size(max = 500)
        String message

) {}