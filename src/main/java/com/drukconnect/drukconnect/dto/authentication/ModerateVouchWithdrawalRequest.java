package com.drukconnect.drukconnect.dto.authentication;

import com.drukconnect.drukconnect.enums.authentication.VouchWithdrawalAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerateVouchWithdrawalRequest(

        @NotNull
        VouchWithdrawalAction action,

        @Size(
                max = 500,
                message = "Admin reason must not exceed 500 characters"
        )
        String reason

) {
}