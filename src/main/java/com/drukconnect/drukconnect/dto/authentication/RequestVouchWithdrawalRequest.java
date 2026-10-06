package com.drukconnect.drukconnect.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestVouchWithdrawalRequest(

        @NotBlank
        @Size(
                max = 500,
                message = "Withdrawal reason must not exceed 500 characters"
        )
        String reason

) {
}