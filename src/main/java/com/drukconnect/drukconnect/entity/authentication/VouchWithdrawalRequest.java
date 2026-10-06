package com.drukconnect.drukconnect.entity.authentication;

import com.drukconnect.drukconnect.entity.authentication.User;

import com.drukconnect.drukconnect.enums.authentication.VouchWithdrawalStatus;
import jakarta.persistence.*;

import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "vouch_withdrawal_requests"
)
public class VouchWithdrawalRequest {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(
            columnDefinition = "CHAR(36)"
    )
    private UUID id;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "vouch_id",
            nullable = false
    )
    private Vouch vouch;


    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "requested_by_user_id",
            nullable = false
    )
    private User requestedBy;


    @Column(
            name = "reason",
            nullable = false,
            length = 500
    )
    private String reason;


    @Enumerated(
            EnumType.STRING
    )
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private VouchWithdrawalStatus status;


    @Column(
            name = "requested_at",
            nullable = false
    )
    private Instant requestedAt;


    @Column(
            name = "moderated_at"
    )
    private Instant moderatedAt;


    @ManyToOne(
            fetch = FetchType.LAZY
    )
    @JoinColumn(
            name = "moderated_by_user_id"
    )
    private User moderatedBy;


    @Column(
            name = "admin_reason",
            length = 500
    )
    private String adminReason;


    @PrePersist
    void prePersist() {

        if (
                requestedAt == null
        ) {

            requestedAt =
                    Instant.now();
        }

        if (
                status == null
        ) {

            status =
                    VouchWithdrawalStatus.PENDING;
        }
    }


    public UUID getId() {
        return id;
    }

    public Vouch getVouch() {
        return vouch;
    }

    public void setVouch(
            Vouch vouch
    ) {
        this.vouch = vouch;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(
            User requestedBy
    ) {
        this.requestedBy = requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(
            String reason
    ) {
        this.reason = reason;
    }

    public VouchWithdrawalStatus getStatus() {
        return status;
    }

    public void setStatus(
            VouchWithdrawalStatus status
    ) {
        this.status = status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(
            Instant moderatedAt
    ) {
        this.moderatedAt = moderatedAt;
    }

    public User getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(
            User moderatedBy
    ) {
        this.moderatedBy = moderatedBy;
    }

    public String getAdminReason() {
        return adminReason;
    }

    public void setAdminReason(
            String adminReason
    ) {
        this.adminReason = adminReason;
    }
}