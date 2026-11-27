package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.VouchRequestEntity;
import com.drukconnect.drukconnect.enums.authentication.VouchRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VouchRequestRepository extends JpaRepository<VouchRequestEntity, UUID> {
    org.springframework.data.domain.Page<VouchRequestEntity> findByRequesterId(
            UUID requesterId, org.springframework.data.domain.Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VouchRequestEntity> findByIdAndStatus(UUID id, VouchRequestStatus status);

    List<VouchRequestEntity> findByTargetIdOrderByRequestedAtDesc(
            UUID targetId
    );

    boolean existsByRequesterIdAndTargetIdAndStatus(
            UUID requesterId,
            UUID targetId,
            VouchRequestStatus status
    );

    long countByRequesterIdAndStatus(
            UUID requesterId,
            VouchRequestStatus status
    );
}
