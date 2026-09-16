package com.drukconnect.drukconnect.entity.listing;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.listing.ReviewStatus;

import jakarta.persistence.*;

import lombok.Data;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "listing_reviews")
@Data
public class ListingReview {

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

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "reviewer_user_id",
            nullable = false
    )
    private User reviewer;

    @Column(
            nullable = false
    )
    private Integer rating;

    @Column(
            name = "comment_text",
            length = 2000
    )
    private String comment;

    @ElementCollection
    @CollectionTable(
            name = "review_tags",
            joinColumns =
            @JoinColumn(name = "review_id")
    )
    @Column(
            name = "tag",
            nullable = false
    )
    private Set<String> tags =
            new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private ReviewStatus status =
            ReviewStatus.PENDING;

    @Column(
            name = "submitted_at",
            nullable = false
    )
    private Instant submittedAt;

    @Column(
            name = "moderated_at"
    )
    private Instant moderatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "moderated_by_user_id"
    )
    private User moderatedBy;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @PrePersist
    void prePersist() {

        submittedAt =
                Instant.now();

        if (status == null) {
            status =
                    ReviewStatus.PENDING;
        }
    }

    public UUID getId() {
        return id;
    }

    public Listing getListing() {
        return listing;
    }

    public void setListing(
            Listing listing
    ) {
        this.listing = listing;
    }

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(
            User reviewer
    ) {
        this.reviewer = reviewer;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(
            Integer rating
    ) {
        this.rating = rating;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(
            Set<String> tags
    ) {
        this.tags = tags;
    }

    public ReviewStatus getStatus() {
        return status;
    }

    public void setStatus(
            ReviewStatus status
    ) {
        this.status = status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(
            Instant moderatedAt
    ) {
        this.moderatedAt =
                moderatedAt;
    }

    public User getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(
            User moderatedBy
    ) {
        this.moderatedBy =
                moderatedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(
            String rejectionReason
    ) {
        this.rejectionReason =
                rejectionReason;
    }
}