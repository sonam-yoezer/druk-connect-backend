package com.drukconnect.drukconnect.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record ListerRestrictionStatusResponse(

        UUID userId,

        boolean restricted,

        boolean canLogin,

        boolean canBrowseListings,

        boolean canCreateListings,

        UUID restrictionId,

        UUID reportId,

        String reference,

        UUID sourceListingId,

        String sourceListingTitle,

        String reason,

        Instant restrictedAt,

        boolean appealAllowed,

        String latestAppealStatus,

        String message

) {
}