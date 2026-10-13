package com.drukconnect.drukconnect.repository.moderation;

import com.drukconnect.drukconnect.entity.moderation.ListerScamAppeal;
import com.drukconnect.drukconnect.enums.moderation.ScamAppealStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ListerScamAppealRepository
        extends JpaRepository<ListerScamAppeal, UUID> {


    boolean existsByListerIdAndStatus(
            UUID listerUserId,
            ScamAppealStatus status
    );


    Optional<ListerScamAppeal>
    findTopByListerIdOrderBySubmittedAtDesc(
            UUID listerUserId
    );


    Page<ListerScamAppeal> findAllByStatus(
            ScamAppealStatus status,
            Pageable pageable
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
            FROM ListerScamAppeal a
            WHERE a.id = :appealId
            """)
    Optional<ListerScamAppeal> findByIdForUpdate(
            @Param("appealId")
            UUID appealId
    );
}