package com.drukconnect.drukconnect.dto.vouchrecovery;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRecoveryVouchRequest(

        @NotNull
        UUID buyerUserId,

        @Size(max = 500)
        String message
) {
}
