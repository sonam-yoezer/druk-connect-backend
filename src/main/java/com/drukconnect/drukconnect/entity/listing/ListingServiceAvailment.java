package com.drukconnect.drukconnect.entity.listing;

import com.drukconnect.drukconnect.entity.authentication.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_service_availments", uniqueConstraints = @UniqueConstraint(name = "uk_service_availment_listing_buyer", columnNames = {"listing_id", "buyer_user_id"}))
@Getter @Setter
public class ListingServiceAvailment {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false) private Listing listing;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_user_id", nullable = false) private User buyer;
    @Column(name = "availed_at", nullable = false) private Instant availedAt;
}
