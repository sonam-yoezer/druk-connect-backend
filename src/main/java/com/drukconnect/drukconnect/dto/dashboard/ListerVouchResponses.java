package com.drukconnect.drukconnect.dto.dashboard;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;

public final class ListerVouchResponses {
    private ListerVouchResponses() {}
    public record Member(UUID userId, String name, String profileImageUrl, String location,
            String bio, String email, String phoneNumber) {}
    public record Received(UUID vouchId, Member member, Instant vouchedAt) {}
    public record Sent(UUID requestId, String memberName, String profileImageUrl,
            String status, Instant requestedAt, Instant respondedAt) {}
    public record Candidate(UUID userId, String name, String profileImageUrl, String location,
            String email, boolean alreadyVouched, boolean requestPending) {}
    public record Results<T>(List<T> items, int currentPage, int totalPages, long totalElements,
            boolean hasNext, boolean hasPrevious) {
        public static <T> Results<T> from(Page<T> page) {
            return new Results<>(page.getContent(), page.getNumber(), page.getTotalPages(),
                    page.getTotalElements(), page.hasNext(), page.hasPrevious());
        }
    }
}
