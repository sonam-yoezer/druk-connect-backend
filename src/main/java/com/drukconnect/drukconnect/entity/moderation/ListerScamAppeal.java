package com.drukconnect.drukconnect.entity.moderation;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.moderation.ScamAppealStatus;

import jakarta.persistence.*;

import lombok.Data;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "lister_scam_appeals",
        indexes = {

                @Index(
                        name = "idx_lister_scam_appeal_user",
                        columnList = "lister_user_id,status"
                ),

                @Index(
                        name = "idx_lister_scam_appeal_status",
                        columnList = "status,submitted_at"
                )
        }
)
public class ListerScamAppeal {

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
     * LISTER appealing restriction.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "lister_user_id",
            nullable = false
    )
    private User lister;


    /*
     * Restriction being appealed.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "restriction_id",
            nullable = false
    )
    private ListerPostingRestriction restriction;


    /*
     * Original report that caused current restriction.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "source_report_id",
            nullable = false
    )
    private ListerScamReport sourceReport;


    @Column(
            name = "appeal_reason",
            nullable = false,
            length = 2000
    )
    private String appealReason;


    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ScamAppealStatus status =
            ScamAppealStatus.PENDING;


    @Column(
            name = "submitted_at",
            nullable = false,
            updatable = false
    )
    private Instant submittedAt;


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

        if (submittedAt == null) {
            submittedAt = Instant.now();
        }

        if (status == null) {
            status = ScamAppealStatus.PENDING;
        }
    }
}