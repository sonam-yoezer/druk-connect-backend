package com.drukconnect.drukconnect.dto.profile;
import com.drukconnect.drukconnect.enums.authentication.OtpChannel;
import jakarta.validation.constraints.*;
public record StartContactChangeRequest(@NotNull OtpChannel channel, @NotBlank @Size(max = 254) String destination,
        @NotBlank @Size(max = 256) String currentPassword) {}
