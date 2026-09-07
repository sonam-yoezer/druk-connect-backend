package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.VouchRequestEntity;
import com.drukconnect.drukconnect.enums.VouchRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VouchRequestRepository extends JpaRepository<VouchRequestEntity, UUID> {
    boolean existsByRequesterIdAndTargetIdAndStatus(UUID requesterId, UUID targetId, VouchRequestStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<VouchRequestEntity> findByIdAndStatus(UUID id, VouchRequestStatus status);

    List<VouchRequestEntity> findByTargetIdOrderByRequestedAtDesc(
            UUID targetId
    );
}
