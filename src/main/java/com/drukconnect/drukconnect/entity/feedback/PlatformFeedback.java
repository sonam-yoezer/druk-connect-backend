package com.drukconnect.drukconnect.entity.feedback;

import com.drukconnect.drukconnect.entity.authentication.User;

import com.drukconnect.drukconnect.enums.feedback.*;

import jakarta.persistence.*;

import lombok.Data;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Data
@Table(
        name = "platform_feedback",
        indexes = {

                @Index(
                        name = "idx_feedback_status",
                        columnList = "status,submitted_at"
                ),

                @Index(
                        name = "idx_feedback_type",
                        columnList = "feedback_type"
                ),

                @Index(
                        name = "idx_feedback_source",
                        columnList = "source"
                ),

                @Index(
                        name = "idx_feedback_submitter_type",
                        columnList = "submitter_access_type"
                ),

                @Index(
                        name = "idx_feedback_theme",
                        columnList = "theme_key"
                )
        }
)
public class PlatformFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(
            name = "id",
            nullable = false,
            updatable = false,
            columnDefinition = "CHAR(36)"
    )
    private UUID id;


    /*
     * MICRO or FORM
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "source",
            nullable = false,
            length = 20
    )
    private FeedbackSource source;


    /*
     * MICRO / BUG / SUGGESTION / COMPLAINT / OTHER
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "feedback_type",
            nullable = false,
            length = 30
    )
    private FeedbackType feedbackType;


    /*
     * User says what perspective they are writing from.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "perspective",
            nullable = false,
            length = 30
    )
    private FeedbackPerspective perspective;


    /*
     * Only used for MICRO feedback.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "micro_rating",
            length = 20
    )
    private MicroFeedbackRating microRating;

    @Column(
            name = "message",
            columnDefinition = "TEXT"
    )
    private String message;


    /*
     * For anonymous form submissions:
     *
     * submittedBy = null
     * submitterNameSnapshot = null
     * submitterEmailSnapshot = null
     *
     * We may still retain the non-identifying account type
     * LISTER/BUYER for aggregate admin statistics.
     */
    @Column(
            name = "anonymous",
            nullable = false
    )
    private boolean anonymous;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "submitted_by_user_id"
    )
    private User submittedBy;


    @Column(
            name = "submitter_name_snapshot",
            length = 200
    )
    private String submitterNameSnapshot;


    @Column(
            name = "submitter_email_snapshot",
            length = 255
    )
    private String submitterEmailSnapshot;


    /*
     * LISTER / BUYER
     */
    @Column(
            name = "submitter_access_type",
            length = 30
    )
    private String submitterAccessType;


    /*
     * Optional screenshot.
     */
    @Column(
            name = "screenshot_path",
            length = 1000
    )
    private String screenshotPath;


    @Column(
            name = "screenshot_original_filename",
            length = 255
    )
    private String screenshotOriginalFilename;


    @Column(
            name = "screenshot_content_type",
            length = 100
    )
    private String screenshotContentType;


    /*
     * Admin workflow:
     *
     * NEW
     * IN_PROGRESS
     * RESOLVED
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private FeedbackStatus status =
            FeedbackStatus.NEW;


    /*
     * Optional theme classification.
     *
     * Example:
     * FREE_LISTING_PRICE
     *
     * "Price field confusing when posting free listings"
     */
    @Column(
            name = "theme_key",
            length = 100
    )
    private String themeKey;


    @Column(
            name = "theme_label",
            length = 300
    )
    private String themeLabel;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "status_updated_by_user_id"
    )
    private User statusUpdatedBy;


    @Column(
            name = "submitted_at",
            nullable = false
    )
    private Instant submittedAt;


    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;


    @Column(
            name = "resolved_at"
    )
    private Instant resolvedAt;


    @PrePersist
    void prePersist() {

        Instant now =
                Instant.now();

        if (submittedAt == null) {
            submittedAt = now;
        }

        updatedAt = now;

        if (status == null) {
            status = FeedbackStatus.NEW;
        }
    }


    @PreUpdate
    void preUpdate() {

        updatedAt =
                Instant.now();
    }
}