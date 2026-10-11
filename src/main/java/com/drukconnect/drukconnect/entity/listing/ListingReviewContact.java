package com.drukconnect.drukconnect.entity.listing;

import com.drukconnect.drukconnect.entity.authentication.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_review_contacts", uniqueConstraints = @UniqueConstraint(name = "uk_review_contact_listing_buyer", columnNames = {"listing_id", "buyer_user_id"}))
@Getter @Setter
public class ListingReviewContact {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_user_id", nullable = false)
    private User buyer;
    @Column(name = "contacted_at", nullable = false)
    private Instant contactedAt;
}
