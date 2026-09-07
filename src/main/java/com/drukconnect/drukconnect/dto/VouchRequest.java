package com.drukconnect.drukconnect.dto;

import com.drukconnect.drukconnect.entity.User;
import com.drukconnect.drukconnect.enums.VouchRequestStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "vouch_requests",
        indexes = {
                @Index(
                        name = "idx_vouch_request_requester",
                        columnList = "requester_user_id,status,requested_at"
                ),
                @Index(
                        name = "idx_vouch_request_target",
                        columnList = "target_user_id,status,requested_at"
                )
        }
)
public class VouchRequest {

    @Id
    @GeneratedValue(
            strategy = GenerationType.UUID
    )
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36)"
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "requester_user_id",
            nullable = false
    )
    private User requesterUser;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "target_user_id",
            nullable = false
    )
    private User targetUser;

    @Column(
            name = "message",
            nullable = false,
            length = 500
    )
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private VouchRequestStatus status =
            VouchRequestStatus.PENDING;

    @Column(
            name = "requested_at",
            nullable = false,
            updatable = false
    )
    private Instant requestedAt;

    @Column(
            name = "responded_at"
    )
    private Instant respondedAt;

    @Column(
            name = "cancelled_at"
    )
    private Instant cancelledAt;

    @PrePersist
    public void prePersist() {

        if (requestedAt == null) {
            requestedAt = Instant.now();
        }

        if (status == null) {
            status = VouchRequestStatus.PENDING;
        }
    }

}