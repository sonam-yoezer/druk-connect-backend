package com.drukconnect.drukconnect.dto.vouchrecovery;

import java.util.List;

public record PagedRecoveryBuyerResponse(

        List<RecoveryBuyerResponse> buyers,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean hasNext,

        boolean hasPrevious

) {
}