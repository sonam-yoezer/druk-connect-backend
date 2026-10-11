package com.drukconnect.drukconnect.dto.listing;
import java.util.List;
public record PagedServiceAvailmentsResponse(List<ServiceAvailmentResponse> availments,
    int currentPage, int pageSize, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
