package com.drukconnect.drukconnect.entity.moderation;

import com.drukconnect.drukconnect.entity.authentication.User;

import jakarta.persistence.*;

import lombok.Data;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "lister_posting_restrictions",
        uniqueConstraints = {

                @UniqueConstraint(
                        name = "uk_lister_posting_restriction_user",
                        columnNames = "lister_user_id"
                )
        },
        indexes = {

                @Index(
                        name = "idx_lister_posting_restriction_active",
                        columnList = "lister_user_id,active"
                )
        }
)
public class ListerPostingRestriction {

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
     * LISTER whose ability to create listings is restricted.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "lister_user_id",
            nullable = false
    )
    private User lister;


    /*
     * Report which most recently caused the restriction.
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
            name = "active",
            nullable = false
    )
    private boolean active = true;


    @Column(
            name = "reason",
            nullable = false,
            length = 1000
    )
    private String reason;


    @Column(
            name = "restricted_at",
            nullable = false
    )
    private Instant restrictedAt;


    @Column(
            name = "cleared_at"
    )
    private Instant clearedAt;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cleared_by_user_id"
    )
    private User clearedBy;


    @Column(
            name = "clearance_reason",
            length = 1000
    )
    private String clearanceReason;


    @Version
    private Long version;


    @PrePersist
    void prePersist() {

        if (restrictedAt == null) {
            restrictedAt = Instant.now();
        }
    }
}