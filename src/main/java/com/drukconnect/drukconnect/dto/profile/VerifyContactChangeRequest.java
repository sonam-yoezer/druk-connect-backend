package com.drukconnect.drukconnect.dto.profile;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record VerifyContactChangeRequest(@NotNull UUID challengeId, @NotBlank @Pattern(regexp = "[0-9]{6}") String otp) {}
