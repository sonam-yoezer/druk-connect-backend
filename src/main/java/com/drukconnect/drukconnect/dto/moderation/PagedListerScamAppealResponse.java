package com.drukconnect.drukconnect.dto.moderation;

import java.util.List;

public record PagedListerScamAppealResponse(

        List<ListerScamAppealResponse> appeals,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean hasNext,

        boolean hasPrevious

) {
}