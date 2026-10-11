package com.drukconnect.drukconnect.entity.listing;

import com.drukconnect.drukconnect.entity.authentication.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_review_requests", uniqueConstraints = @UniqueConstraint(name = "uk_review_request_listing_buyer", columnNames = {"listing_id", "buyer_user_id"}))
@Getter @Setter
public class ListingReviewRequest {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_user_id", nullable = false)
    private User buyer;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
    @Column(name = "email_sent_at")
    private Instant emailSentAt;
    @Column(name = "completed_at")
    private Instant completedAt;
}
