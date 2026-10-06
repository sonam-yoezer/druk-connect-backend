package com.drukconnect.drukconnect.dto.vouchrecovery;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRecoveryVouchInvitationRequest(

        @NotBlank
        @Email
        String email,

        @Size(max = 500)
        String message

) {
}