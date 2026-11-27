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


    long countByReporterId(UUID reporterId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"reporter", "reportedLister"})
    Page<ListerScamReport> findAllByReporterId(UUID reporterId, Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"reporter", "reportedLister"})
    Page<ListerScamReport> findAllByReporterIdAndStatus(UUID reporterId, ScamReportStatus status, Pageable pageable);

    interface ReporterStats {
        long getTotalReports();
        long getPendingReports();
        long getApprovedReports();
        long getRejectedReports();
        java.time.Instant getFirstReportedAt();
        java.time.Instant getLastReportedAt();
    }

    @Query("""
        select count(r) as totalReports,
               coalesce(sum(case when r.status = 'PENDING' then 1 else 0 end), 0) as pendingReports,
               coalesce(sum(case when r.status = 'APPROVED' then 1 else 0 end), 0) as approvedReports,
               coalesce(sum(case when r.status = 'REJECTED' then 1 else 0 end), 0) as rejectedReports,
               min(r.reportedAt) as firstReportedAt, max(r.reportedAt) as lastReportedAt
        from ListerScamReport r where r.reporter.id = :reporterId
        """)
    ReporterStats getReporterStats(@Param("reporterId") UUID reporterId);

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