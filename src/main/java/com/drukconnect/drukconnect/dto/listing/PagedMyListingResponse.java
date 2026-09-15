package com.drukconnect.drukconnect.dto.listing;

import java.util.List;

public record PagedMyListingResponse(

        List<MyListingResponse> listings,

        int currentPage,

        int pageSize,

        long totalElements,

        int totalPages,

        boolean hasNext,

        boolean hasPrevious

) {
}
