package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.Vouch;
import com.drukconnect.drukconnect.enums.VouchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VouchRepository extends JpaRepository<Vouch, UUID> {
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
}
