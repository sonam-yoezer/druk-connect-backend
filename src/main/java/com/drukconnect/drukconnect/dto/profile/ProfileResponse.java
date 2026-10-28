package com.drukconnect.drukconnect.dto.profile;

import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProfileResponse(UUID id, String firstName, String lastName, String email,
        String phoneNumber, AccessTypeEnum accessType, List<String> roles, String bio,
        String location, String profileImageUrl, Instant emailVerifiedAt, Instant phoneVerifiedAt) {}
