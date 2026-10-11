package com.drukconnect.drukconnect.dto.listing;
import java.util.List;
public record PagedReviewInvitationsResponse(List<ReviewInvitationResponse> requests, int currentPage,
    int pageSize, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
