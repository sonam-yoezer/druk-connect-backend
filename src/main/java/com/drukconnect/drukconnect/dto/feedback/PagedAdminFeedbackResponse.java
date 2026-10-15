package com.drukconnect.drukconnect.dto.feedback;

import java.util.List;

public record PagedAdminFeedbackResponse(

        List<AdminFeedbackItemResponse> feedback,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean hasNext,

        boolean hasPrevious

) {
}