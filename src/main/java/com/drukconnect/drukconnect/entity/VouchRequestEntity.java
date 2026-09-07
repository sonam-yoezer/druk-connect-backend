package com.drukconnect.drukconnect.entity;

import com.drukconnect.drukconnect.enums.VouchRequestStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "vouch_requests")
public class VouchRequestEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name="requester_user_id", nullable=false)
    private User requester;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name="target_user_id", nullable=false)
    private User target;

    @Column(nullable=false, length=500) private String message;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private VouchRequestStatus status;
    @Column(name="requested_at", nullable=false) private Instant requestedAt;
    @Column(name="responded_at") private Instant respondedAt;
    @Column(name="cancelled_at") private Instant cancelledAt;

    @Column(
            name = "response_token_hash",
            length = 64
    )
    private String responseTokenHash;

    @Column(
            name = "response_token_expires_at"
    )
    private Instant responseTokenExpiresAt;

    @Column(
            name = "notification_sent_at"
    )
    private Instant notificationSentAt;

    @PrePersist void pre(){ if(requestedAt==null)requestedAt=Instant.now(); if(status==null)status=VouchRequestStatus.PENDING; }


 }
