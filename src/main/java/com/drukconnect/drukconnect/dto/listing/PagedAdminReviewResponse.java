package com.drukconnect.drukconnect.dto.listing;

import java.util.List;

public record PagedAdminReviewResponse(

        List<AdminReviewResponse> reviews,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages

) {
}