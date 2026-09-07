package com.drukconnect.drukconnect.dto;

import com.drukconnect.drukconnect.enums.AccessTypeEnum;
import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(max = 20) String phoneNumber,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotNull
        AccessTypeEnum accessType,
        @AssertTrue(message = "Community guidelines must be accepted") boolean communityGuidelinesAccepted,

        String vouchInvitationToken
) {}
