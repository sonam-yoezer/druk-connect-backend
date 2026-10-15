package com.drukconnect.drukconnect.repository.feedback;

import com.drukconnect.drukconnect.entity.feedback.PlatformFeedback;

import com.drukconnect.drukconnect.enums.feedback.FeedbackSource;
import com.drukconnect.drukconnect.enums.feedback.MicroFeedbackRating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PlatformFeedbackRepository
        extends
        JpaRepository<PlatformFeedback, UUID>,
        JpaSpecificationExecutor<PlatformFeedback> {


    long countBySubmittedAtGreaterThanEqual(
            Instant submittedAt
    );


    long countBySubmitterAccessType(
            String accessType
    );


    long countBySourceAndSubmittedAtGreaterThanEqual(
            FeedbackSource source,
            Instant submittedAt
    );


    long countBySourceAndMicroRatingAndSubmittedAtGreaterThanEqual(
            FeedbackSource source,
            MicroFeedbackRating rating,
            Instant submittedAt
    );


    long countByStatusNot(
            com.drukconnect.drukconnect.enums.feedback.FeedbackStatus status
    );


    @Query("""
            SELECT
                f.themeKey,
                f.themeLabel,
                COUNT(f)
            FROM PlatformFeedback f
            WHERE f.themeKey IS NOT NULL
              AND f.themeLabel IS NOT NULL
              AND f.submittedAt >= :from
            GROUP BY
                f.themeKey,
                f.themeLabel
            ORDER BY COUNT(f) DESC
            """)
    List<Object[]> findThemeStats(
            Instant from
    );
}