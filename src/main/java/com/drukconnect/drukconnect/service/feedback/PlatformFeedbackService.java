package com.drukconnect.drukconnect.service.feedback;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;

import com.drukconnect.drukconnect.dto.feedback.AdminFeedbackItemResponse;
import com.drukconnect.drukconnect.dto.feedback.CreateMicroFeedbackRequest;
import com.drukconnect.drukconnect.dto.feedback.CreatePlatformFeedbackRequest;
import com.drukconnect.drukconnect.dto.feedback.FeedbackDashboardResponse;
import com.drukconnect.drukconnect.dto.feedback.FeedbackSubmissionResponse;
import com.drukconnect.drukconnect.dto.feedback.FeedbackThemeStatResponse;
import com.drukconnect.drukconnect.dto.feedback.PagedAdminFeedbackResponse;
import com.drukconnect.drukconnect.dto.feedback.UpdateFeedbackStatusRequest;
import com.drukconnect.drukconnect.dto.feedback.UpdateFeedbackThemeRequest;

import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.feedback.PlatformFeedback;

import com.drukconnect.drukconnect.enums.authentication.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;

import com.drukconnect.drukconnect.enums.feedback.FeedbackPerspective;
import com.drukconnect.drukconnect.enums.feedback.FeedbackSource;
import com.drukconnect.drukconnect.enums.feedback.FeedbackStatus;
import com.drukconnect.drukconnect.enums.feedback.FeedbackType;
import com.drukconnect.drukconnect.enums.feedback.MicroFeedbackRating;

import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.feedback.PlatformFeedbackRepository;

import com.drukconnect.drukconnect.service.authentication.AuditService;

import com.drukconnect.drukconnect.util.PlatformFeedbackNotificationSender;

import jakarta.persistence.criteria.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import org.springframework.data.jpa.domain.Specification;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;

import java.time.temporal.TemporalAdjusters;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class PlatformFeedbackService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PlatformFeedbackService.class
            );


    private final PlatformFeedbackRepository
            feedbackRepository;

    private final UserRepository
            userRepository;

    private final FeedbackScreenshotStorageService
            screenshotStorageService;

    private final AuditService
            auditService;

    private final PlatformFeedbackNotificationSender
            notificationSender;


    public PlatformFeedbackService(

            PlatformFeedbackRepository feedbackRepository,

            UserRepository userRepository,

            FeedbackScreenshotStorageService screenshotStorageService,

            AuditService auditService,

            PlatformFeedbackNotificationSender notificationSender

    ) {

        this.feedbackRepository =
                feedbackRepository;

        this.userRepository =
                userRepository;

        this.screenshotStorageService =
                screenshotStorageService;

        this.auditService =
                auditService;

        this.notificationSender =
                notificationSender;
    }


    /*
     * =========================================================
     * SUBMIT MICRO FEEDBACK
     * =========================================================
     *
     * Example:
     *
     * How was posting a service today?
     *
     * EASY
     * CONFUSING
     *
     * Optional comment.
     *
     * EMAIL:
     * - confirmation to USER
     * - notification to ADMIN
     * =========================================================
     */
    @Transactional
    public FeedbackSubmissionResponse submitMicroFeedback(

            UUID userId,

            CreateMicroFeedbackRequest request,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * USER
         * =====================================================
         */
        User user =
                activeUser(
                        userId
                );


        /*
         * =====================================================
         * CREATE FEEDBACK
         * =====================================================
         */
        PlatformFeedback feedback =
                new PlatformFeedback();


        feedback.setSource(
                FeedbackSource.MICRO
        );


        feedback.setFeedbackType(
                FeedbackType.MICRO
        );


        /*
         * Micro-feedback perspective comes automatically
         * from the account type.
         */
        if (
                user.getAccessType()
                        == AccessTypeEnum.LISTER
        ) {

            feedback.setPerspective(
                    FeedbackPerspective.LISTER
            );

        } else {

            feedback.setPerspective(
                    FeedbackPerspective.BROWSING_USER
            );
        }


        feedback.setMicroRating(
                request.rating()
        );

        feedback.setMessage(
                normalizeNullable(
                        request.comment()
                )
        );


        /*
         * Micro-feedback is not anonymous.
         */
        feedback.setAnonymous(
                false
        );


        /*
         * Save identifiable submitter.
         */
        applyKnownUser(
                feedback,
                user
        );


        feedback.setStatus(
                FeedbackStatus.NEW
        );


        feedback =
                feedbackRepository
                        .saveAndFlush(
                                feedback
                        );


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */
        auditService.log(
                "PLATFORM_MICRO_FEEDBACK_SUBMITTED",

                userId,

                null,

                null,

                null,

                meta,

                "{\"feedbackId\":\""
                        + feedback.getId()
                        + "\",\"rating\":\""
                        + feedback.getMicroRating()
                        + "\"}"
        );


        /*
         * =====================================================
         * EMAIL USER
         * =====================================================
         */
        safeSendFeedbackSubmittedToUser(
                user,
                feedback
        );


        /*
         * =====================================================
         * EMAIL ADMIN
         * =====================================================
         */
        safeSendFeedbackSubmittedToAdmin(
                feedback
        );


        return submissionResponse(
                feedback
        );
    }


    /*
     * =========================================================
     * SUBMIT FULL FEEDBACK FORM
     * =========================================================
     *
     * BUG
     * SUGGESTION
     * COMPLAINT
     * OTHER
     *
     * Optional:
     * - screenshot
     * - anonymous
     *
     * EMAIL:
     *
     * Normal feedback:
     * USER + ADMIN
     *
     * Anonymous:
     * ADMIN ONLY
     *
     * Sending an email back to an anonymous submitter would
     * conflict with the "cannot follow up" behavior.
     * =========================================================
     */
    @Transactional
    public FeedbackSubmissionResponse submitFeedback(

            UUID userId,

            CreatePlatformFeedbackRequest request,

            MultipartFile screenshot,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * USER
         * =====================================================
         */
        User user =
                activeUser(
                        userId
                );


        /*
         * =====================================================
         * MICRO MUST USE MICRO ENDPOINT
         * =====================================================
         */
        if (
                request.type()
                        == FeedbackType.MICRO
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_FEEDBACK_TYPE",
                    "MICRO feedback must use the micro-feedback endpoint"
            );
        }


        /*
         * =====================================================
         * CREATE FEEDBACK
         * =====================================================
         */
        PlatformFeedback feedback =
                new PlatformFeedback();


        feedback.setSource(
                FeedbackSource.FORM
        );


        feedback.setFeedbackType(
                request.type()
        );


        feedback.setPerspective(
                request.perspective()
        );


        feedback.setMessage(
                request.message()
                        .trim()
        );


        feedback.setAnonymous(
                request.anonymous()
        );


        /*
         * =====================================================
         * ACCESS TYPE SNAPSHOT
         * =====================================================
         *
         * Used only for aggregate statistics:
         *
         * "62% from listers"
         * "38% from browsing users"
         *
         * Identity is still removed when anonymous.
         */
        feedback.setSubmitterAccessType(
                user.getAccessType()
                        .name()
        );


        /*
         * =====================================================
         * KNOWN USER
         * =====================================================
         */
        if (
                !request.anonymous()
        ) {

            applyKnownUser(
                    feedback,
                    user
            );
        }


        feedback.setStatus(
                FeedbackStatus.NEW
        );


        /*
         * Save first to generate feedback UUID.
         */
        feedback =
                feedbackRepository
                        .saveAndFlush(
                                feedback
                        );


        /*
         * =====================================================
         * SCREENSHOT
         * =====================================================
         */
        if (
                screenshot != null
                        &&
                        !screenshot.isEmpty()
        ) {

            String screenshotPath =
                    screenshotStorageService
                            .store(
                                    feedback.getId(),
                                    screenshot
                            );


            feedback.setScreenshotPath(
                    screenshotPath
            );


            feedback.setScreenshotOriginalFilename(
                    screenshot.getOriginalFilename()
            );


            feedback.setScreenshotContentType(
                    screenshot.getContentType()
            );


            feedback =
                    feedbackRepository
                            .saveAndFlush(
                                    feedback
                            );
        }


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         *
         * Anonymous feedback does not store the user ID
         * as the audit actor.
         */
        auditService.log(
                "PLATFORM_FEEDBACK_SUBMITTED",

                request.anonymous()
                        ? null
                        : userId,

                null,

                null,

                null,

                meta,

                "{\"feedbackId\":\""
                        + feedback.getId()
                        + "\",\"type\":\""
                        + feedback.getFeedbackType()
                        + "\",\"anonymous\":"
                        + request.anonymous()
                        + "}"
        );


        /*
         * =====================================================
         * USER EMAIL
         * =====================================================
         *
         * Only when NOT anonymous.
         */
        if (
                !feedback.isAnonymous()
        ) {

            safeSendFeedbackSubmittedToUser(
                    user,
                    feedback
            );
        }


        /*
         * =====================================================
         * ADMIN EMAIL
         * =====================================================
         *
         * Always notify admin.
         */
        safeSendFeedbackSubmittedToAdmin(
                feedback
        );


        return submissionResponse(
                feedback
        );
    }


    /*
     * =========================================================
     * ADMIN GET FEEDBACK
     * =========================================================
     *
     * Filters:
     *
     * type
     * perspective
     * source
     * status
     * submittedAs
     * anonymous
     * unresolved
     *
     * GET requests do NOT send email.
     *
     * Otherwise simply opening the admin dashboard would
     * continuously generate emails.
     * =========================================================
     */
    @Transactional(readOnly = true)
    public PagedAdminFeedbackResponse getFeedback(

            FeedbackType type,

            FeedbackPerspective perspective,

            FeedbackSource source,

            FeedbackStatus status,

            String submittedAs,

            Boolean anonymous,

            boolean unresolved,

            int page,

            int size

    ) {

        int safePage =
                Math.max(
                        page,
                        1
                );


        int safeSize =
                Math.min(
                        Math.max(
                                size,
                                1
                        ),
                        100
                );


        Specification<PlatformFeedback> specification =
                (
                        root,
                        query,
                        cb
                ) -> {

                    List<Predicate> predicates =
                            new ArrayList<>();


                    /*
                     * TYPE
                     */
                    if (
                            type != null
                    ) {

                        predicates.add(
                                cb.equal(
                                        root.get(
                                                "feedbackType"
                                        ),
                                        type
                                )
                        );
                    }


                    /*
                     * PERSPECTIVE
                     */
                    if (
                            perspective != null
                    ) {

                        predicates.add(
                                cb.equal(
                                        root.get(
                                                "perspective"
                                        ),
                                        perspective
                                )
                        );
                    }


                    /*
                     * SOURCE
                     */
                    if (
                            source != null
                    ) {

                        predicates.add(
                                cb.equal(
                                        root.get(
                                                "source"
                                        ),
                                        source
                                )
                        );
                    }


                    /*
                     * STATUS
                     */
                    if (
                            status != null
                    ) {

                        predicates.add(
                                cb.equal(
                                        root.get(
                                                "status"
                                        ),
                                        status
                                )
                        );
                    }


                    /*
                     * UNRESOLVED
                     */
                    if (
                            unresolved
                    ) {

                        predicates.add(
                                cb.notEqual(
                                        root.get(
                                                "status"
                                        ),
                                        FeedbackStatus.RESOLVED
                                )
                        );
                    }


                    /*
                     * LISTER / BUYER
                     */
                    if (
                            submittedAs != null
                                    &&
                                    !submittedAs.isBlank()
                    ) {

                        predicates.add(
                                cb.equal(
                                        root.get(
                                                "submitterAccessType"
                                        ),
                                        submittedAs
                                                .trim()
                                                .toUpperCase(
                                                        Locale.ROOT
                                                )
                                )
                        );
                    }


                    /*
                     * ANONYMOUS
                     */
                    if (
                            anonymous != null
                    ) {

                        predicates.add(
                                cb.equal(
                                        root.get(
                                                "anonymous"
                                        ),
                                        anonymous
                                )
                        );
                    }


                    return cb.and(
                            predicates.toArray(
                                    Predicate[]::new
                            )
                    );
                };


        Page<PlatformFeedback> result =
                feedbackRepository
                        .findAll(

                                specification,

                                PageRequest.of(

                                        safePage - 1,

                                        safeSize,

                                        Sort.by(
                                                Sort.Direction.DESC,
                                                "submittedAt"
                                        )
                                )
                        );


        return new PagedAdminFeedbackResponse(

                result.getContent()
                        .stream()
                        .map(
                                this::adminResponse
                        )
                        .toList(),

                result.getNumber() + 1,

                result.getSize(),

                result.getTotalElements(),

                result.getTotalPages(),

                result.hasNext(),

                result.hasPrevious()
        );
    }


    /*
     * =========================================================
     * ADMIN UPDATE STATUS
     * =========================================================
     *
     * NEW
     * IN_PROGRESS
     * RESOLVED
     *
     * EMAIL:
     *
     * USER:
     * "Your feedback status has changed."
     *
     * ADMIN:
     * confirmation that they changed the status.
     * =========================================================
     */
    @Transactional
    public AdminFeedbackItemResponse updateStatus(

            UUID adminUserId,

            UUID feedbackId,

            UpdateFeedbackStatusRequest request,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * ADMIN
         * =====================================================
         */
        User admin =
                activeUser(
                        adminUserId
                );


        /*
         * =====================================================
         * FEEDBACK
         * =====================================================
         */
        PlatformFeedback feedback =
                feedbackRepository
                        .findById(
                                feedbackId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "FEEDBACK_NOT_FOUND",
                                        "Feedback was not found"
                                )
                        );


        /*
         * =====================================================
         * SAME STATUS
         * =====================================================
         *
         * Avoid pointless status updates and duplicate emails.
         */
        if (
                feedback.getStatus()
                        == request.status()
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "FEEDBACK_STATUS_ALREADY_SET",
                    "Feedback already has this status"
            );
        }


        /*
         * =====================================================
         * UPDATE STATUS
         * =====================================================
         */
        feedback.setStatus(
                request.status()
        );


        feedback.setStatusUpdatedBy(
                admin
        );


        /*
         * =====================================================
         * RESOLUTION TIME
         * =====================================================
         */
        if (
                request.status()
                        == FeedbackStatus.RESOLVED
        ) {

            feedback.setResolvedAt(
                    Instant.now()
            );

        } else {

            feedback.setResolvedAt(
                    null
            );
        }


        feedback =
                feedbackRepository
                        .saveAndFlush(
                                feedback
                        );


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */
        auditService.log(
                "PLATFORM_FEEDBACK_STATUS_UPDATED",

                adminUserId,

                null,

                feedback.getSubmittedBy()
                        == null
                        ? null
                        : feedback
                        .getSubmittedBy()
                        .getId(),

                null,

                meta,

                "{\"feedbackId\":\""
                        + feedback.getId()
                        + "\",\"status\":\""
                        + request.status()
                        + "\"}"
        );


        /*
         * =====================================================
         * USER EMAIL
         * =====================================================
         *
         * Anonymous feedback cannot be followed up.
         */
        if (
                !feedback.isAnonymous()
                        &&
                        feedback.getSubmittedBy()
                                != null
        ) {

            safeSendStatusUpdatedToUser(
                    feedback.getSubmittedBy(),
                    feedback
            );
        }


        /*
         * =====================================================
         * ADMIN EMAIL
         * =====================================================
         *
         * Send to admin who performed the action.
         */
        safeSendStatusUpdatedToAdmin(
                admin,
                feedback
        );


        return adminResponse(
                feedback
        );
    }


    /*
     * =========================================================
     * ADMIN ASSIGN / CHANGE THEME
     * =========================================================
     *
     * Example:
     *
     * FREE_LISTING_PRICE
     *
     * Price field confusing when posting free listings
     *
     * EMAIL:
     *
     * USER:
     * generic "your feedback has been reviewed"
     *
     * ADMIN:
     * detailed theme update confirmation
     * =========================================================
     */
    @Transactional
    public AdminFeedbackItemResponse updateTheme(

            UUID adminUserId,

            UUID feedbackId,

            UpdateFeedbackThemeRequest request,

            RequestMetadata meta

    ) {

        /*
         * =====================================================
         * ADMIN
         * =====================================================
         */
        User admin =
                activeUser(
                        adminUserId
                );


        /*
         * =====================================================
         * FEEDBACK
         * =====================================================
         */
        PlatformFeedback feedback =
                feedbackRepository
                        .findById(
                                feedbackId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "FEEDBACK_NOT_FOUND",
                                        "Feedback was not found"
                                )
                        );


        String themeKey =
                request.themeKey()
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );


        String themeLabel =
                request.themeLabel()
                        .trim();


        /*
         * =====================================================
         * UPDATE THEME
         * =====================================================
         */
        feedback.setThemeKey(
                themeKey
        );


        feedback.setThemeLabel(
                themeLabel
        );


        feedback =
                feedbackRepository
                        .saveAndFlush(
                                feedback
                        );


        /*
         * =====================================================
         * AUDIT
         * =====================================================
         */
        auditService.log(
                "PLATFORM_FEEDBACK_THEME_UPDATED",

                adminUserId,

                null,

                feedback.getSubmittedBy()
                        == null
                        ? null
                        : feedback
                        .getSubmittedBy()
                        .getId(),

                null,

                meta,

                "{\"feedbackId\":\""
                        + feedback.getId()
                        + "\",\"themeKey\":\""
                        + feedback.getThemeKey()
                        + "\"}"
        );


        /*
         * =====================================================
         * USER EMAIL
         * =====================================================
         */
        if (
                !feedback.isAnonymous()
                        &&
                        feedback.getSubmittedBy()
                                != null
        ) {

            safeSendThemeUpdatedToUser(
                    feedback.getSubmittedBy(),
                    feedback
            );
        }


        /*
         * =====================================================
         * ADMIN EMAIL
         * =====================================================
         */
        safeSendThemeUpdatedToAdmin(
                admin,
                feedback
        );


        return adminResponse(
                feedback
        );
    }


    /*
     * =========================================================
     * ADMIN DASHBOARD
     * =========================================================
     *
     * Provides:
     *
     * New this week
     * % from LISTERs
     * % from browsing users
     * EASY micro-feedback %
     * unresolved count
     * recurring themes
     *
     * GET operation only.
     * No email.
     * =========================================================
     */
    @Transactional(readOnly = true)
    public FeedbackDashboardResponse dashboard() {

        /*
         * =====================================================
         * DATE RANGES
         * =====================================================
         */
        ZoneId zone =
                ZoneOffset.UTC;


        LocalDate today =
                LocalDate.now(
                        zone
                );


        Instant startOfWeek =
                today
                        .with(
                                TemporalAdjusters
                                        .previousOrSame(
                                                DayOfWeek.MONDAY
                                        )
                        )
                        .atStartOfDay(
                                zone
                        )
                        .toInstant();


        Instant thirtyDaysAgo =
                Instant.now()
                        .minus(
                                Duration.ofDays(
                                        30
                                )
                        );


        Instant startOfMonth =
                today
                        .withDayOfMonth(
                                1
                        )
                        .atStartOfDay(
                                zone
                        )
                        .toInstant();


        /*
         * =====================================================
         * NEW THIS WEEK
         * =====================================================
         */
        long newThisWeek =
                feedbackRepository
                        .countBySubmittedAtGreaterThanEqual(
                                startOfWeek
                        );


        /*
         * =====================================================
         * LISTER VS BROWSING USER
         * =====================================================
         */
        long listerCount =
                feedbackRepository
                        .countBySubmitterAccessType(
                                AccessTypeEnum.LISTER
                                        .name()
                        );


        long buyerCount =
                feedbackRepository
                        .countBySubmitterAccessType(
                                AccessTypeEnum.BUYER
                                        .name()
                        );


        long roleTotal =
                listerCount
                        + buyerCount;


        double fromListersPercent =
                percentage(
                        listerCount,
                        roleTotal
                );


        double fromBrowsingUsersPercent =
                percentage(
                        buyerCount,
                        roleTotal
                );


        /*
         * =====================================================
         * MICRO FEEDBACK - LAST 30 DAYS
         * =====================================================
         */
        long microFeedbackCount =
                feedbackRepository
                        .countBySourceAndSubmittedAtGreaterThanEqual(
                                FeedbackSource.MICRO,
                                thirtyDaysAgo
                        );


        long easyFeedbackCount =
                feedbackRepository
                        .countBySourceAndMicroRatingAndSubmittedAtGreaterThanEqual(
                                FeedbackSource.MICRO,
                                MicroFeedbackRating.EASY,
                                thirtyDaysAgo
                        );


        double easyMicroFeedbackPercent =
                percentage(
                        easyFeedbackCount,
                        microFeedbackCount
                );


        /*
         * =====================================================
         * UNRESOLVED
         * =====================================================
         */
        long unresolvedCount =
                feedbackRepository
                        .countByStatusNot(
                                FeedbackStatus.RESOLVED
                        );


        /*
         * =====================================================
         * RECURRING THEMES THIS MONTH
         * =====================================================
         */
        List<FeedbackThemeStatResponse> recurringThemes =
                feedbackRepository
                        .findThemeStats(
                                startOfMonth
                        )
                        .stream()
                        .limit(
                                5
                        )
                        .map(row ->
                                new FeedbackThemeStatResponse(

                                        (String) row[0],

                                        (String) row[1],

                                        (Long) row[2]
                                )
                        )
                        .toList();


        return new FeedbackDashboardResponse(

                newThisWeek,

                fromListersPercent,

                fromBrowsingUsersPercent,

                easyMicroFeedbackPercent,

                unresolvedCount,

                recurringThemes
        );
    }


    /*
     * =========================================================
     * SAFE MAIL
     *
     * FEEDBACK SUBMITTED -> USER
     * =========================================================
     */
    private void safeSendFeedbackSubmittedToUser(

            User user,

            PlatformFeedback feedback

    ) {

        try {

            notificationSender
                    .sendFeedbackSubmittedToUser(
                            user,
                            feedback
                    );

        } catch (Exception ex) {

            /*
             * Email failure must NOT rollback
             * feedback submission.
             */
            log.error(
                    "Failed to send feedback submission email to user. "
                            + "feedbackId={}, userId={}",
                    feedback.getId(),
                    user.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE MAIL
     *
     * FEEDBACK SUBMITTED -> ADMIN
     * =========================================================
     */
    private void safeSendFeedbackSubmittedToAdmin(
            PlatformFeedback feedback
    ) {

        try {

            notificationSender
                    .sendFeedbackSubmittedToAdmin(
                            feedback
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send new feedback email to admin. "
                            + "feedbackId={}",
                    feedback.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE MAIL
     *
     * STATUS UPDATED -> USER
     * =========================================================
     */
    private void safeSendStatusUpdatedToUser(

            User user,

            PlatformFeedback feedback

    ) {

        try {

            notificationSender
                    .sendStatusUpdatedToUser(
                            user,
                            feedback
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send feedback status update email to user. "
                            + "feedbackId={}, userId={}",
                    feedback.getId(),
                    user.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE MAIL
     *
     * STATUS UPDATED -> ADMIN WHO PERFORMED ACTION
     * =========================================================
     */
    private void safeSendStatusUpdatedToAdmin(

            User admin,

            PlatformFeedback feedback

    ) {

        try {

            notificationSender
                    .sendStatusUpdatedToAdmin(
                            admin,
                            feedback
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send feedback status update email to admin. "
                            + "feedbackId={}, adminUserId={}",
                    feedback.getId(),
                    admin.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE MAIL
     *
     * THEME UPDATED -> USER
     * =========================================================
     */
    private void safeSendThemeUpdatedToUser(

            User user,

            PlatformFeedback feedback

    ) {

        try {

            notificationSender
                    .sendThemeUpdatedToUser(
                            user,
                            feedback
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send feedback theme/update email to user. "
                            + "feedbackId={}, userId={}",
                    feedback.getId(),
                    user.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * SAFE MAIL
     *
     * THEME UPDATED -> ADMIN
     * =========================================================
     */
    private void safeSendThemeUpdatedToAdmin(

            User admin,

            PlatformFeedback feedback

    ) {

        try {

            notificationSender
                    .sendThemeUpdatedToAdmin(
                            admin,
                            feedback
                    );

        } catch (Exception ex) {

            log.error(
                    "Failed to send feedback theme update email to admin. "
                            + "feedbackId={}, adminUserId={}",
                    feedback.getId(),
                    admin.getId(),
                    ex
            );
        }
    }


    /*
     * =========================================================
     * ACTIVE USER
     * =========================================================
     */
    private User activeUser(
            UUID userId
    ) {

        User user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User was not found"
                                )
                        );


        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "ACCOUNT_NOT_ACTIVE",
                    "User account is not active"
            );
        }


        return user;
    }


    /*
     * =========================================================
     * APPLY IDENTIFIABLE USER INFORMATION
     * =========================================================
     */
    private void applyKnownUser(

            PlatformFeedback feedback,

            User user

    ) {

        feedback.setSubmittedBy(
                user
        );


        feedback.setSubmitterNameSnapshot(
                fullName(
                        user
                )
        );


        feedback.setSubmitterEmailSnapshot(
                user.getEmail()
        );


        feedback.setSubmitterAccessType(
                user.getAccessType()
                        .name()
        );
    }


    /*
     * =========================================================
     * USER FULL NAME
     * =========================================================
     */
    private String fullName(
            User user
    ) {

        String firstName =
                user.getFirstName()
                        == null
                        ? ""
                        : user.getFirstName()
                        .trim();


        String lastName =
                user.getLastName()
                        == null
                        ? ""
                        : user.getLastName()
                        .trim();


        String fullName =
                (
                        firstName
                                + " "
                                + lastName
                ).trim();


        if (
                fullName.isBlank()
        ) {

            return "DrukConnect member";
        }


        return fullName;
    }


    /*
     * =========================================================
     * NORMALIZE OPTIONAL TEXT
     * =========================================================
     */
    private String normalizeNullable(
            String value
    ) {

        if (
                value == null
                        ||
                        value.isBlank()
        ) {

            return null;
        }


        return value.trim();
    }


    /*
     * =========================================================
     * PERCENTAGE
     * =========================================================
     */
    private double percentage(

            long part,

            long total

    ) {

        if (
                total <= 0
        ) {

            return 0.0;
        }


        double percentage =
                (
                        (double) part
                                /
                                (double) total
                )
                        * 100.0;


        /*
         * One decimal place.
         */
        return Math.round(
                percentage
                        * 10.0
        ) / 10.0;
    }


    /*
     * =========================================================
     * USER SUBMISSION RESPONSE
     * =========================================================
     */
    private FeedbackSubmissionResponse submissionResponse(
            PlatformFeedback feedback
    ) {

        return new FeedbackSubmissionResponse(

                feedback.getId(),

                feedback.getSource()
                        .name(),

                feedback.getFeedbackType()
                        .name(),

                feedback.getStatus()
                        .name(),

                feedback.getSubmittedAt(),

                "Thank you for your feedback."
        );
    }


    /*
     * =========================================================
     * ADMIN FEEDBACK RESPONSE
     * =========================================================
     */
    private AdminFeedbackItemResponse adminResponse(
            PlatformFeedback feedback
    ) {

        /*
         * =====================================================
         * CAN ADMIN FOLLOW UP?
         * =====================================================
         */
        boolean canFollowUp =
                !feedback.isAnonymous()
                        &&
                        feedback.getSubmittedBy()
                                != null;


        UUID submittedByUserId =
                canFollowUp

                        ? feedback
                        .getSubmittedBy()
                        .getId()

                        : null;


        String submitterName;


        String submitterEmail;


        if (
                feedback.isAnonymous()
        ) {

            submitterName =
                    "Anonymous";

            submitterEmail =
                    null;

        } else {

            submitterName =
                    feedback.getSubmitterNameSnapshot();

            submitterEmail =
                    feedback.getSubmitterEmailSnapshot();
        }


        return new AdminFeedbackItemResponse(

                feedback.getId(),

                feedback.getSource()
                        .name(),

                feedback.getFeedbackType()
                        .name(),

                feedback.getPerspective()
                        .name(),

                feedback.getMicroRating()
                        == null

                        ? null

                        : feedback
                        .getMicroRating()
                        .name(),

                feedback.getMessage(),

                feedback.isAnonymous(),

                submittedByUserId,

                submitterName,

                submitterEmail,

                feedback.getSubmitterAccessType(),

                canFollowUp,

                feedback.getScreenshotPath(),

                feedback.getStatus()
                        .name(),

                feedback.getThemeKey(),

                feedback.getThemeLabel(),

                feedback.getSubmittedAt(),

                feedback.getUpdatedAt(),

                feedback.getResolvedAt()
        );
    }
}