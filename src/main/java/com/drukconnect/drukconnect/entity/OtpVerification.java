package com.drukconnect.drukconnect.entity;

import com.drukconnect.drukconnect.enums.OtpChannel;
import com.drukconnect.drukconnect.enums.OtpDeliveryStatus;
import com.drukconnect.drukconnect.enums.OtpPurpose;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "otp_verifications")
public class OtpVerification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(nullable = false, length = 254)
    private String destination;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 20)
    private OtpDeliveryStatus deliveryStatus;

    @Column(name = "delivery_error", length = 500)
    private String deliveryError;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "requested_ip", length = 64)
    private String requestedIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (deliveryStatus == null) deliveryStatus = OtpDeliveryStatus.PENDING;
    }

}
