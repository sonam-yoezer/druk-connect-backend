package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.VouchWithdrawalRequest;
import com.drukconnect.drukconnect.enums.authentication.VouchWithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VouchWithdrawalRequestRepository
        extends JpaRepository<VouchWithdrawalRequest, UUID> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"requestedBy", "vouch", "vouch.vouchedUser"})
    Page<VouchWithdrawalRequest> findAllByStatus(VouchWithdrawalStatus status, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"requestedBy", "vouch", "vouch.vouchedUser"})
    Page<VouchWithdrawalRequest> findAllByVouchVouchedUserId(UUID listerId, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"requestedBy", "vouch", "vouch.vouchedUser"})
    Page<VouchWithdrawalRequest> findAllByVouchVouchedUserIdAndStatus(UUID listerId, VouchWithdrawalStatus status, Pageable pageable);

    interface ListerStats {
        long getTotalRequests();
        long getUniqueRequesters();
        long getPendingRequests();
        long getApprovedRequests();
        long getRejectedRequests();
        java.time.Instant getFirstRequestedAt();
        java.time.Instant getLastRequestedAt();
    }

    @org.springframework.data.jpa.repository.Query("""
        select count(w) as totalRequests, count(distinct w.requestedBy.id) as uniqueRequesters,
            coalesce(sum(case when w.status = 'PENDING' then 1 else 0 end), 0) as pendingRequests,
            coalesce(sum(case when w.status = 'APPROVED' then 1 else 0 end), 0) as approvedRequests,
            coalesce(sum(case when w.status = 'REJECTED' then 1 else 0 end), 0) as rejectedRequests,
            min(w.requestedAt) as firstRequestedAt, max(w.requestedAt) as lastRequestedAt
        from VouchWithdrawalRequest w where w.vouch.vouchedUser.id = :listerId
        """)
    ListerStats getListerStats(@org.springframework.data.repository.query.Param("listerId") UUID listerId);

    boolean existsByVouchIdAndStatus(
            UUID vouchId,
            VouchWithdrawalStatus status
    );

    Optional<VouchWithdrawalRequest>
    findByIdAndStatus(
            UUID id,
            VouchWithdrawalStatus status
    );

    Page<VouchWithdrawalRequest>
    findByStatusOrderByRequestedAtAsc(
            VouchWithdrawalStatus status,
            Pageable pageable
    );
}
