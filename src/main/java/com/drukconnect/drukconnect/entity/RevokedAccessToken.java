package com.drukconnect.drukconnect.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name = "revoked_access_tokens")
public class RevokedAccessToken {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, unique = true, length = 36) private String jti;
    @Column(name="session_id", nullable=false) private UUID sessionId;
    @Column(name="user_id", nullable=false) private UUID userId;
    @Column(name="expires_at", nullable=false) private Instant expiresAt;
    @Column(name="revoked_at", nullable=false) private Instant revokedAt;
    @Column(nullable=false, length=100) private String reason;
    @PrePersist void pre(){ if(revokedAt==null) revokedAt=Instant.now(); }
}
