package com.drukconnect.drukconnect.dto.authentication;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public final class AdminWithdrawalHistoryResponses {
    private AdminWithdrawalHistoryResponses() {}
    public record Item(UUID withdrawalRequestId, UUID vouchId, UUID buyerUserId, String buyerName,
            String buyerEmail, String buyerPhoneNumber, UUID listerUserId, String listerName,
            String listerEmail, String listerPhoneNumber, String reason, String status,
            Instant vouchedAt, Instant requestedAt, Instant moderatedAt, String adminReason,
            long listerRequestCount, long listerUniqueRequesterCount) {}
    public record Results(List<Item> requests, int currentPage, int pageSize, long totalElements,
            int totalPages, boolean hasNext, boolean hasPrevious) {
        public static Results from(Page<Item> page) {
            return new Results(page.getContent(), page.getNumber() + 1, page.getSize(),
                    page.getTotalElements(), page.getTotalPages(), page.hasNext(), page.hasPrevious());
        }
    }
    public record History(UUID listerUserId, String listerName, long totalRequests, long uniqueRequesters,
            long pendingRequests, long approvedRequests, long rejectedRequests,
            Instant firstRequestedAt, Instant lastRequestedAt, Results history) {}
}
