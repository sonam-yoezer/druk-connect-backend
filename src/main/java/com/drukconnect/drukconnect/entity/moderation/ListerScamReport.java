package com.drukconnect.drukconnect.entity.moderation;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.moderation.ScamReportStatus;

import jakarta.persistence.*;

import lombok.Data;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "lister_scam_reports",
        indexes = {

                @Index(
                        name = "idx_lister_scam_report_lister",
                        columnList = "reported_lister_id,status"
                ),

                @Index(
                        name = "idx_lister_scam_report_listing",
                        columnList = "source_listing_id"
                ),

                @Index(
                        name = "idx_lister_scam_report_status",
                        columnList = "status,reported_at"
                )
        }
)
public class ListerScamReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36)"
    )
    private UUID id;


    /*
     * =========================================================
     * LISTER BEING REPORTED
     * =========================================================
     *
     * THIS is the actual moderation target.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reported_lister_id",
            nullable = false
    )
    private User reportedLister;


    /*
     * Person submitting the report.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reporter_user_id",
            nullable = false
    )
    private User reporter;


    /*
     * =========================================================
     * SOURCE LISTING
     * =========================================================
     *
     * Only evidence/reference showing where the report
     * originated.
     *
     * Restriction is NOT tied to this listing.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "source_listing_id",
            nullable = false,
            columnDefinition = "CHAR(36)"
    )
    private UUID sourceListingId;


    /*
     * Snapshot so admin can still see what listing caused
     * the report even if listing title later changes.
     */
    @Column(
            name = "source_listing_title",
            nullable = false,
            length = 255
    )
    private String sourceListingTitle;


    @Column(
            name = "reason",
            nullable = false,
            length = 500
    )
    private String reason;


    @Column(
            name = "details",
            length = 2000
    )
    private String details;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ScamReportStatus status =
            ScamReportStatus.PENDING;


    @Column(
            name = "reported_at",
            nullable = false,
            updatable = false
    )
    private Instant reportedAt;


    /*
     * Admin who reviewed report.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "moderated_by_user_id"
    )
    private User moderatedBy;


    @Column(
            name = "moderated_at"
    )
    private Instant moderatedAt;


    @Column(
            name = "moderation_reason",
            length = 1000
    )
    private String moderationReason;


    @PrePersist
    void prePersist() {

        if (reportedAt == null) {
            reportedAt = Instant.now();
        }

        if (status == null) {
            status = ScamReportStatus.PENDING;
        }
    }
}