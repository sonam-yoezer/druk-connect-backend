package com.drukconnect.drukconnect.entity.listing;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.listing.ListingAvailability;
import com.drukconnect.drukconnect.enums.listing.ListingPricingType;
import com.drukconnect.drukconnect.enums.listing.ListingStatus;

import jakarta.persistence.*;

import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "listings")
@Data
public class Listing {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(
            name = "id",
            columnDefinition = "CHAR(36)"
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "lister_user_id",
            nullable = false
    )
    private User lister;

    @Column(
            name = "listing_title",
            nullable = false,
            length = 180
    )
    private String listingTitle;

    @Column(
            name = "listing_category",
            nullable = false,
            length = 120
    )
    private String listingCategory;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "service_type",
            nullable = false,
            length = 120
    )
    private String serviceType;

    @ElementCollection
    @CollectionTable(
            name = "listing_dietary_options",
            joinColumns =
            @JoinColumn(name = "listing_id")
    )
    @Column(
            name = "dietary_option",
            nullable = false
    )
    private Set<String> dietaryOptions =
            new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(
            name = "availability",
            nullable = false,
            length = 20
    )
    private ListingAvailability availability;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "pricing_type",
            nullable = false,
            length = 20
    )
    private ListingPricingType pricingType;

    @Column(
            name = "rate_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal rateAmount;

    @Column(
            name = "currency_code",
            nullable = false,
            length = 3
    )
    private String currencyCode = "AUD";

    @Column(
            name = "views",
            nullable = false
    )
    private Long views = 0L;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private ListingStatus status =
            ListingStatus.ACTIVE;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;

    @Column(
            name = "suburb",
            nullable = false,
            length = 120
    )
    private String suburb;

    @Column(
            name = "state",
            nullable = false,
            length = 100
    )
    private String state;

    @Column(
            name = "postcode",
            nullable = false,
            length = 20
    )
    private String postcode;

    @PrePersist
    void prePersist() {

        Instant now =
                Instant.now();

        createdAt = now;
        updatedAt = now;

        if (views == null) {
            views = 0L;
        }

        if (status == null) {
            status =
                    ListingStatus.ACTIVE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt =
                Instant.now();
    }

}