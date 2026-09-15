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
            name = "city",
            nullable = false,
            length = 120
    )
    private String city;

    @Column(
            name = "description",
            nullable = false,
            columnDefinition = "TEXT"
    )
    private String description;

    @Column(
            name = "cuisine",
            nullable = false,
            length = 120
    )
    private String cuisine;

    @Column(
            name = "service_type",
            nullable = false,
            length = 120
    )
    private String serviceType;

    @Column(
            name = "minimum_order",
            nullable = false
    )
    private Integer minimumOrder;

    @Column(
            name = "serves",
            nullable = false
    )
    private Integer serves;

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

    public UUID getId() {
        return id;
    }

    public User getLister() {
        return lister;
    }

    public void setLister(User lister) {
        this.lister = lister;
    }

    public String getListingTitle() {
        return listingTitle;
    }

    public void setListingTitle(String listingTitle) {
        this.listingTitle = listingTitle;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCuisine() {
        return cuisine;
    }

    public void setCuisine(String cuisine) {
        this.cuisine = cuisine;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public Integer getMinimumOrder() {
        return minimumOrder;
    }

    public void setMinimumOrder(Integer minimumOrder) {
        this.minimumOrder = minimumOrder;
    }

    public Integer getServes() {
        return serves;
    }

    public void setServes(Integer serves) {
        this.serves = serves;
    }

    public Set<String> getDietaryOptions() {
        return dietaryOptions;
    }

    public void setDietaryOptions(
            Set<String> dietaryOptions
    ) {
        this.dietaryOptions =
                dietaryOptions;
    }

    public ListingAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(
            ListingAvailability availability
    ) {
        this.availability =
                availability;
    }

    public ListingPricingType getPricingType() {
        return pricingType;
    }

    public void setPricingType(
            ListingPricingType pricingType
    ) {
        this.pricingType =
                pricingType;
    }

    public BigDecimal getRateAmount() {
        return rateAmount;
    }

    public void setRateAmount(
            BigDecimal rateAmount
    ) {
        this.rateAmount =
                rateAmount;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(
            String currencyCode
    ) {
        this.currencyCode =
                currencyCode;
    }

    public Long getViews() {
        return views;
    }

    public void setViews(Long views) {
        this.views = views;
    }

    public ListingStatus getStatus() {
        return status;
    }

    public void setStatus(
            ListingStatus status
    ) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}