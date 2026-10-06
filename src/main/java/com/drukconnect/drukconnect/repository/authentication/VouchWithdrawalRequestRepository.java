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
