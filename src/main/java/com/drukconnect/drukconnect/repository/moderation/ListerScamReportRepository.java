package com.drukconnect.drukconnect.repository.moderation;

import com.drukconnect.drukconnect.entity.moderation.ListerScamReport;
import com.drukconnect.drukconnect.enums.moderation.ScamReportStatus;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ListerScamReportRepository
        extends JpaRepository<ListerScamReport, UUID> {


    /*
     * Same user cannot spam duplicate pending reports
     * against same source listing.
     */
    boolean existsBySourceListingIdAndReporterIdAndStatus(
            UUID sourceListingId,
            UUID reporterId,
            ScamReportStatus status
    );


    Page<ListerScamReport> findAllByStatus(
            ScamReportStatus status,
            Pageable pageable
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r
            FROM ListerScamReport r
            WHERE r.id = :reportId
            """)
    Optional<ListerScamReport> findByIdForUpdate(
            @Param("reportId")
            UUID reportId
    );
}