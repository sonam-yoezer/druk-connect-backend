package com.drukconnect.drukconnect.dto.listing;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
public record CreateReviewInvitationRequest(@NotNull UUID buyerUserId) {}
