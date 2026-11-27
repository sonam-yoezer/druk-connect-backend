package com.drukconnect.drukconnect.repository.authentication;

import com.drukconnect.drukconnect.entity.authentication.Vouch;
import com.drukconnect.drukconnect.enums.authentication.VouchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VouchRepository extends JpaRepository<Vouch, UUID> {
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"voucherUser"})
    org.springframework.data.domain.Page<Vouch> findByVouchedUserIdAndStatus(
            UUID userId, VouchStatus status, org.springframework.data.domain.Pageable pageable);

    boolean existsByVoucherUserIdAndVouchedUserIdAndStatus(UUID voucherId, UUID vouchedId, VouchStatus status);

    @Query("""
            SELECT COUNT(v)
            FROM Vouch v
            WHERE v.vouchedUser.id = :userId
            AND v.status = 'ACTIVE'
            """)
    long countActiveVouchesByUserId(
            @Param("userId") UUID userId
    );

    List<Vouch> findByVouchedUserIdAndStatusOrderByVouchedAtDesc(
            UUID vouchedUserId,
            VouchStatus status
    );

    List<Vouch> findByVoucherUserIdAndStatusOrderByVouchedAtDesc(
            UUID voucherUserId,
            VouchStatus status
    );

    Optional<Vouch> findByIdAndVoucherUserIdAndStatus(
            UUID id,
            UUID voucherUserId,
            VouchStatus status
    );

    Optional<Vouch> findByVoucherUserIdAndVouchedUserId(
            UUID voucherUserId,
            UUID vouchedUserId
    );

}
