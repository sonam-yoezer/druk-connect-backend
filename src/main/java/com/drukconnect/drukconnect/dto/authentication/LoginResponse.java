package com.drukconnect.drukconnect.dto.authentication;

import com.drukconnect.drukconnect.enums.authentication.LoginStatus;

public record LoginResponse(

        LoginStatus loginStatus,

        TokenResponse tokens,

        String vouchRecoveryToken,

        Long vouchRecoveryTokenExpiresIn,

        Long activeVouchCount,

        Long requiredVouchCount,

        Long vouchesNeeded,

        UserSummary user,

        String message

) {
}