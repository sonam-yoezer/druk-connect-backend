package com.drukconnect.drukconnect.entity.listing;

import jakarta.persistence.*;

import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "listing_images")
public class ListingImage {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(
            columnDefinition = "CHAR(36)"
    )
    private UUID id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "listing_id",
            nullable = false
    )
    private Listing listing;

    @Column(
            name = "image_path",
            nullable = false,
            length = 500
    )
    private String imagePath;

    @Column(
            name = "original_filename",
            length = 255
    )
    private String originalFilename;

    @Column(
            name = "content_type",
            nullable = false,
            length = 100
    )
    private String contentType;

    @Column(
            name = "sort_order",
            nullable = false
    )
    private Integer sortOrder;

    @Column(
            name = "created_at",
            nullable = false
    )
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        createdAt =
                Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(Listing listing) {
        this.listing = listing;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(
            String imagePath
    ) {
        this.imagePath = imagePath;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(
            String originalFilename
    ) {
        this.originalFilename =
                originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(
            String contentType
    ) {
        this.contentType =
                contentType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(
            Integer sortOrder
    ) {
        this.sortOrder =
                sortOrder;
    }
}