package com.drukconnect.drukconnect.dto.listing;
import java.util.List;
public record PagedReviewRecipientsResponse(List<ReviewRecipientResponse> buyers, int currentPage,
    int pageSize, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
