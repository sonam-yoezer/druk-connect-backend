package com.drukconnect.drukconnect.dto.authentication;

import java.util.List;

public record PagedAdminVouchWithdrawalResponse(

        List<AdminVouchWithdrawalResponse> requests,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean hasNext,

        boolean hasPrevious

) {
}
