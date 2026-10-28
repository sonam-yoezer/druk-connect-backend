package com.drukconnect.drukconnect.entity.authentication;

import com.drukconnect.drukconnect.enums.authentication.OtpChannel;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "profile_contact_changes", uniqueConstraints = @UniqueConstraint(name = "uk_profile_contact_user_channel", columnNames = {"user_id", "channel"}))
@Getter @Setter
public class ProfileContactChange {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) private OtpChannel channel;
    @Column(nullable = false, length = 254) private String destination;
    @Column(name = "otp_hash", nullable = false, length = 64) private String otpHash;
    @Column(nullable = false) private int attempts;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "consumed_at") private Instant consumedAt;
}
