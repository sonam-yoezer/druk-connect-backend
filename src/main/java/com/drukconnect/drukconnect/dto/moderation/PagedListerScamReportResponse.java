package com.drukconnect.drukconnect.dto.moderation;

import java.util.List;

public record PagedListerScamReportResponse(

        List<ListerScamReportResponse> reports,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean hasNext,

        boolean hasPrevious

) {
}