package com.drukconnect.drukconnect.entity;

import com.drukconnect.drukconnect.enums.VouchStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(name="vouches", uniqueConstraints={
        @UniqueConstraint(name="uk_voucher_vouched", columnNames={"voucher_user_id","vouched_user_id"}),
        @UniqueConstraint(name="uk_vouch_request", columnNames={"vouch_request_id"})
})
public class Vouch {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="voucher_user_id", nullable=false) private User voucherUser;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="vouched_user_id", nullable=false) private User vouchedUser;
    @OneToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="vouch_request_id", nullable=false) private VouchRequestEntity request;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private VouchStatus status;
    @Column(name="vouched_at", nullable=false) private Instant vouchedAt;
    @Column(name="revoked_at") private Instant revokedAt;
    @PrePersist void pre(){if(vouchedAt==null)vouchedAt=Instant.now(); if(status==null)status=VouchStatus.ACTIVE;}
}
