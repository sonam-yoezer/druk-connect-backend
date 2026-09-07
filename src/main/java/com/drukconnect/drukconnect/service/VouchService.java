package com.drukconnect.drukconnect.service;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.IdentityNormalizer;
import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.dto.*;
import com.drukconnect.drukconnect.entity.User;
import com.drukconnect.drukconnect.entity.Vouch;
import com.drukconnect.drukconnect.entity.VouchInvitation;
import com.drukconnect.drukconnect.entity.VouchRequestEntity;
import com.drukconnect.drukconnect.enums.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.UserStatus;
import com.drukconnect.drukconnect.enums.VouchInvitationStatus;
import com.drukconnect.drukconnect.enums.VouchRequestStatus;
import com.drukconnect.drukconnect.enums.VouchStatus;
import com.drukconnect.drukconnect.repository.UserRepository;
import com.drukconnect.drukconnect.repository.VouchInvitationRepository;
import com.drukconnect.drukconnect.repository.VouchRepository;
import com.drukconnect.drukconnect.repository.VouchRequestRepository;
import com.drukconnect.drukconnect.util.VouchInvitationEmailSender;
import com.drukconnect.drukconnect.util.VouchRequestEmailSender;
import com.drukconnect.drukconnect.util.VouchSuccessEmailSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class VouchService {

    private final VouchRequestRepository requestRepository;
    private final VouchRepository vouchRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final VouchInvitationRepository invitationRepository;
    private final VouchInvitationEmailSender invitationEmailSender;
    private final AppProperties properties;

    @Autowired
    private VouchSuccessEmailSender vouchSuccessEmailSender;

    @Autowired
    private VouchRequestEmailSender vouchRequestEmailSender;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public VouchService(
            VouchRequestRepository requestRepository,
            VouchRepository vouchRepository,
            UserRepository userRepository,
            AuditService auditService,
            VouchInvitationRepository invitationRepository,
            VouchInvitationEmailSender invitationEmailSender,
            AppProperties properties
    ) {

        this.requestRepository =
                requestRepository;

        this.vouchRepository =
                vouchRepository;

        this.userRepository =
                userRepository;

        this.auditService =
                auditService;

        this.invitationRepository =
                invitationRepository;

        this.invitationEmailSender =
                invitationEmailSender;

        this.properties =
                properties;
    }

    /*
     * =========================================================
     * CREATE VOUCH REQUEST
     * =========================================================
     */
    @Transactional
    public VouchRequestResponse create(
            UUID requesterId,
            CreateVouchRequest request,
            RequestMetadata meta
    ) {

        if (
                requesterId.equals(
                        request.targetUserId()
                )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_VOUCH_NOT_ALLOWED",
                    "You cannot ask yourself to vouch for you"
            );
        }

        User requester =
                activeUser(
                        requesterId
                );

        User target =
                activeUser(
                        request.targetUserId()
                );

        /*
         * Only LISTER can request vouch.
         */
        if (
                requester.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Only Lister accounts can request a vouch"
            );
        }

        /*
         * Only BUYER can provide vouch.
         */
        if (
                target.getAccessType()
                        != AccessTypeEnum.BUYER
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "BUYER_REQUIRED",
                    "Vouch requests can only be sent to Buyer accounts"
            );
        }

        /*
         * Already vouched.
         */
        if (
                vouchRepository
                        .existsByVoucherUserIdAndVouchedUserIdAndStatus(
                                target.getId(),
                                requester.getId(),
                                VouchStatus.ACTIVE
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "ALREADY_VOUCHED",
                    "This member has already vouched for you"
            );
        }

        /*
         * Existing pending request.
         */
        if (
                requestRepository
                        .existsByRequesterIdAndTargetIdAndStatus(
                                requesterId,
                                target.getId(),
                                VouchRequestStatus.PENDING
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "VOUCH_REQUEST_EXISTS",
                    "A pending vouch request already exists for this member"
            );
        }

        /*
         * =========================================================
         * CREATE REQUEST
         * =========================================================
         */
        VouchRequestEntity entity =
                new VouchRequestEntity();

        entity.setRequester(
                requester
        );

        entity.setTarget(
                target
        );

        entity.setMessage(
                request.message().trim()
        );

        entity.setStatus(
                VouchRequestStatus.PENDING
        );

        entity =
                requestRepository.saveAndFlush(
                        entity
                );

        /*
         * =========================================================
         * GENERATE EMAIL RESPONSE TOKEN
         * =========================================================
         */
        String rawToken =
                generateInvitationToken();

        entity.setResponseTokenHash(
                hashInvitationToken(
                        rawToken
                )
        );

        entity.setResponseTokenExpiresAt(
                Instant.now()
                        .plus(
                                properties
                                        .vouch()
                                        .responseTokenTtl()
                        )
        );

        entity =
                requestRepository.saveAndFlush(
                        entity
                );

        /*
         * =========================================================
         * SEND EMAIL
         * =========================================================
         */
        try {

            vouchRequestEmailSender.send(
                    requester,
                    target,
                    entity,
                    rawToken
            );

            entity.setNotificationSentAt(
                    Instant.now()
            );

            requestRepository.save(
                    entity
            );

            auditService.log(
                    "VOUCH_REQUEST_EMAIL_SENT",
                    requesterId,
                    null,
                    target.getId(),
                    IdentityNormalizer.maskIdentifier(
                            target.getEmail()
                    ),
                    meta,
                    "{\"vouchRequestId\":\""
                            + entity.getId()
                            + "\"}"
            );

        } catch (Exception ex) {

            /*
             * Do not cancel the request if email fails.
             *
             * Buyer can still see request inside system.
             */
            auditService.log(
                    "VOUCH_REQUEST_EMAIL_FAILED",
                    requesterId,
                    null,
                    target.getId(),
                    IdentityNormalizer.maskIdentifier(
                            target.getEmail()
                    ),
                    meta,
                    "{\"vouchRequestId\":\""
                            + entity.getId()
                            + "\"}"
            );
        }

        auditService.log(
                "VOUCH_REQUESTED",
                requesterId,
                null,
                target.getId(),
                null,
                meta,
                "{\"vouchRequestId\":\""
                        + entity.getId()
                        + "\"}"
        );

        return toResponse(
                entity
        );
    }

    /*
     * =========================================================
     * RESPOND TO VOUCH REQUEST
     * =========================================================
     */
    @Transactional
    public VouchRequestResponse respond(
            UUID currentUserId,
            UUID requestId,
            RespondVouchRequest request,
            RequestMetadata meta
    ) {

        VouchRequestEntity entity =
                requestRepository
                        .findByIdAndStatus(
                                requestId,
                                VouchRequestStatus.PENDING
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "VOUCH_REQUEST_NOT_FOUND",
                                        "Pending vouch request not found"
                                )
                        );

        /*
         * Only requested user can respond.
         */
        if (
                !entity.getTarget()
                        .getId()
                        .equals(
                                currentUserId
                        )
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "NOT_VOUCH_REQUEST_TARGET",
                    "Only the requested member can respond to this vouch request"
            );
        }

        /*
         * Target must still be active.
         */
        User target =
                activeUser(
                        currentUserId
                );

        if (
                target.getAccessType()
                        != AccessTypeEnum.BUYER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "BUYER_REQUIRED",
                    "Only Buyer accounts can provide a vouch"
            );
        }

        Instant now =
                Instant.now();

        entity.setRespondedAt(
                now
        );

        /*
         * =====================================================
         * ACCEPT
         * =====================================================
         */
        if (
                Boolean.TRUE.equals(
                        request.accept()
                )
        ) {

            entity.setStatus(
                    VouchRequestStatus.ACCEPTED
            );

            requestRepository.saveAndFlush(
                    entity
            );

            /*
             * Prevent duplicate active vouch.
             */
            if (
                    !vouchRepository
                            .existsByVoucherUserIdAndVouchedUserIdAndStatus(
                                    entity.getTarget()
                                            .getId(),

                                    entity.getRequester()
                                            .getId(),

                                    VouchStatus.ACTIVE
                            )
            ) {

                Vouch vouch =
                        new Vouch();

                /*
                 * BUYER gives vouch.
                 */
                vouch.setVoucherUser(
                        entity.getTarget()
                );

                /*
                 * LISTER receives vouch.
                 */
                vouch.setVouchedUser(
                        entity.getRequester()
                );

                /*
                 * IMPORTANT:
                 * Your current Vouch entity uses setRequest().
                 */
                vouch.setRequest(
                        entity
                );

                vouch.setStatus(
                        VouchStatus.ACTIVE
                );

                vouch.setVouchedAt(
                        now
                );

                vouchRepository.saveAndFlush(
                        vouch
                );
            }

            auditService.log(
                    "VOUCH_ACCEPTED",
                    currentUserId,
                    null,
                    entity.getRequester()
                            .getId(),
                    null,
                    meta,
                    "{\"vouchRequestId\":\""
                            + entity.getId()
                            + "\"}"
            );

        }

        /*
         * =====================================================
         * DECLINE
         * =====================================================
         */
        else {

            entity.setStatus(
                    VouchRequestStatus.DECLINED
            );

            requestRepository.save(
                    entity
            );

            auditService.log(
                    "VOUCH_DECLINED",
                    currentUserId,
                    null,
                    entity.getRequester()
                            .getId(),
                    null,
                    meta,
                    "{\"vouchRequestId\":\""
                            + entity.getId()
                            + "\"}"
            );
        }

        return toResponse(
                entity
        );
    }

    /*
     * =========================================================
     * SEARCH USERS TO VOUCH
     * =========================================================
     */
    @Transactional(readOnly = true)
    public List<VouchUserSearchResponse> searchUsers(
            UUID requesterId,
            String query
    ) {

        User requester =
                userRepository
                        .findById(requesterId)
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User not found"
                                )
                        );

        if (
                requester.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Only Lister accounts can search for users to request a vouch"
            );
        }

        if (
                query == null
                        ||
                        query.trim().length() < 2
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SEARCH_QUERY_TOO_SHORT",
                    "Enter at least 2 characters"
            );
        }

        return userRepository
                .searchVouchCandidates(
                        requesterId,
                        query.trim(),
                        UserStatus.ACTIVE,
                        AccessTypeEnum.BUYER,
                        PageRequest.of(
                                0,
                                20
                        )
                )
                .stream()
                .map(user ->
                        new VouchUserSearchResponse(
                                user.getId(),
                                user.getFirstName(),
                                user.getLastName(),
                                user.getEmail(),
                                user.getAccessType()
                        )
                )
                .toList();
    }
    /*
     * =========================================================
     * SEND INVITATION EMAIL
     * =========================================================
     */
    @Transactional
    public VouchInvitationResponse inviteByEmail(
            UUID requesterId,
            CreateVouchInvitationRequest request,
            RequestMetadata meta
    ) {

        User requester =
                userRepository
                        .findById(
                                requesterId
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User not found"
                                )
                        );

        /*
         * Only LISTER can invite.
         */
        if (
                requester.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "LISTER_REQUIRED",
                    "Only Lister accounts can send vouch invitations"
            );
        }

        String email =
                IdentityNormalizer.email(
                        request.email()
                );

        /*
         * Prevent inviting yourself.
         */
        if (
                requester.getEmail()
                        .equalsIgnoreCase(
                                email
                        )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "SELF_VOUCH_NOT_ALLOWED",
                    "You cannot send a vouch invitation to yourself"
            );
        }

        /*
         * Existing DrukConnect user?
         *
         * They should use normal vouch request instead.
         */
        User existingUser =
                userRepository
                        .findByEmailIgnoreCase(
                                email
                        )
                        .orElse(null);

        if (
                existingUser != null
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "USER_ALREADY_REGISTERED",
                    "This email already belongs to a DrukConnect user. Please send a normal vouch request instead."
            );
        }

        /*
         * Existing pending invitation?
         */
        if (
                invitationRepository
                        .existsByRequesterUserIdAndInviteeEmailIgnoreCaseAndStatus(
                                requesterId,
                                email,
                                VouchInvitationStatus.PENDING
                        )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INVITATION_ALREADY_PENDING",
                    "A pending invitation has already been sent to this email address"
            );
        }

        /*
         * Generate secure invitation token.
         */
        String rawToken =
                generateInvitationToken();

        VouchInvitation invitation =
                new VouchInvitation();

        invitation.setRequesterUser(
                requester
        );

        invitation.setInviteeEmail(
                email
        );

        invitation.setMessage(
                request.message()
        );

        /*
         * Store only HASH in DB.
         */
        invitation.setTokenHash(
                hashInvitationToken(
                        rawToken
                )
        );

        invitation.setStatus(
                VouchInvitationStatus.PENDING
        );

        invitation.setExpiresAt(
                Instant.now()
                        .plus(
                                properties
                                        .vouch()
                                        .invitationTtl()
                        )
        );

        invitation =
                invitationRepository
                        .saveAndFlush(
                                invitation
                        );

        /*
         * Send actual token in email.
         */
        invitationEmailSender.send(
                requester,
                email,
                rawToken
        );

        invitation.setSentAt(
                Instant.now()
        );

        invitationRepository.save(
                invitation
        );

        auditService.log(
                "VOUCH_INVITATION_SENT",
                requesterId,
                null,
                null,
                IdentityNormalizer.maskIdentifier(
                        email
                ),
                meta,
                null
        );

        return new VouchInvitationResponse(
                invitation.getId(),
                invitation.getInviteeEmail(),
                invitation.getStatus()
                        .name(),
                invitation.getExpiresAt(),
                "Vouch invitation sent successfully."
        );
    }

    /*
     * =========================================================
     * ATTACH INVITATION WHEN INVITED PERSON SIGNS UP
     * =========================================================
     */
    @Transactional
    public void attachInvitationToBuyer(
            String rawToken,
            User buyer
    ) {

        if (
                rawToken == null
                        ||
                        rawToken.isBlank()
        ) {
            return;
        }

        /*
         * Invitation signup must be BUYER.
         */
        if (
                buyer.getAccessType()
                        != AccessTypeEnum.BUYER
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_REQUIRES_BUYER",
                    "A vouch invitation must be registered using a Buyer account"
            );
        }

        String tokenHash =
                hashInvitationToken(
                        rawToken
                );

        VouchInvitation invitation =
                invitationRepository
                        .findByTokenHashAndStatus(
                                tokenHash,
                                VouchInvitationStatus.PENDING
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "INVALID_VOUCH_INVITATION",
                                        "The vouch invitation is invalid"
                                )
                        );

        /*
         * Expired?
         */
        if (
                !invitation.getExpiresAt()
                        .isAfter(
                                Instant.now()
                        )
        ) {

            invitation.setStatus(
                    VouchInvitationStatus.EXPIRED
            );

            invitationRepository.save(
                    invitation
            );

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VOUCH_INVITATION_EXPIRED",
                    "The vouch invitation has expired"
            );
        }

        /*
         * Signup email MUST match invitation email.
         */
        if (
                !invitation.getInviteeEmail()
                        .equalsIgnoreCase(
                                buyer.getEmail()
                        )
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_EMAIL_MISMATCH",
                    "You must register using the email address that received the invitation"
            );
        }

        invitation.setRegisteredUser(
                buyer
        );

        invitationRepository.save(
                invitation
        );
    }

    /*
     * =========================================================
     * COMPLETE INVITATION AFTER BUYER EMAIL VERIFICATION
     * =========================================================
     */
    @Transactional
    public void completeInvitationForBuyer(
            User buyer,
            RequestMetadata meta
    ) {

        /*
         * =========================================================
         * BUYER ONLY
         * =========================================================
         */
        if (
                buyer == null
                        ||
                        buyer.getAccessType()
                                != AccessTypeEnum.BUYER
        ) {
            return;
        }

        /*
         * =========================================================
         * BUYER MUST BE ACTIVE
         * =========================================================
         */
        if (
                buyer.getStatus()
                        != UserStatus.ACTIVE
        ) {
            return;
        }

        /*
         * =========================================================
         * EMAIL MUST BE VERIFIED
         * =========================================================
         */
        if (
                buyer.getEmailVerifiedAt()
                        == null
        ) {
            return;
        }

        /*
         * =========================================================
         * FIND PENDING INVITATION
         * =========================================================
         */
        VouchInvitation invitation =
                invitationRepository
                        .findByRegisteredUserIdAndStatus(
                                buyer.getId(),
                                VouchInvitationStatus.PENDING
                        )
                        .orElse(null);

        if (
                invitation == null
        ) {
            return;
        }

        Instant now =
                Instant.now();

        /*
         * =========================================================
         * CHECK EXPIRATION
         * =========================================================
         */
        if (
                invitation.getExpiresAt() == null
                        ||
                        !invitation.getExpiresAt()
                                .isAfter(
                                        now
                                )
        ) {

            invitation.setStatus(
                    VouchInvitationStatus.EXPIRED
            );

            invitationRepository.saveAndFlush(
                    invitation
            );

            return;
        }

        /*
         * =========================================================
         * ORIGINAL LISTER
         * =========================================================
         */
        User lister =
                invitation.getRequesterUser();

        if (
                lister == null
        ) {

            throw new ApiException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "INVITATION_REQUESTER_MISSING",
                    "Vouch invitation requester could not be found"
            );
        }

        if (
                lister.getAccessType()
                        != AccessTypeEnum.LISTER
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "INVALID_INVITATION_REQUESTER",
                    "The invitation requester is not a Lister"
            );
        }

        /*
         * =========================================================
         * CHECK EXISTING ACTIVE VOUCH
         * =========================================================
         */
        boolean alreadyVouched =
                vouchRepository
                        .existsByVoucherUserIdAndVouchedUserIdAndStatus(
                                buyer.getId(),
                                lister.getId(),
                                VouchStatus.ACTIVE
                        );

        if (
                alreadyVouched
        ) {

            invitation.setStatus(
                    VouchInvitationStatus.ACCEPTED
            );

            invitation.setAcceptedAt(
                    now
            );

            invitationRepository.saveAndFlush(
                    invitation
            );

            return;
        }

        /*
         * =========================================================
         * CREATE ACCEPTED VOUCH REQUEST
         * =========================================================
         */
        VouchRequestEntity vouchRequest =
                new VouchRequestEntity();

        vouchRequest.setRequester(
                lister
        );

        vouchRequest.setTarget(
                buyer
        );

        vouchRequest.setMessage(
                invitation.getMessage() == null
                        ||
                        invitation.getMessage().isBlank()
                        ? "Vouch invitation"
                        : invitation.getMessage()
        );

        vouchRequest.setStatus(
                VouchRequestStatus.ACCEPTED
        );

        vouchRequest.setRequestedAt(
                invitation.getCreatedAt() != null
                        ? invitation.getCreatedAt()
                        : now
        );

        vouchRequest.setRespondedAt(
                now
        );

        vouchRequest =
                requestRepository
                        .saveAndFlush(
                                vouchRequest
                        );

        /*
         * =========================================================
         * CREATE ACTIVE VOUCH
         * =========================================================
         */
        Vouch vouch =
                new Vouch();

        /*
         * BUYER gives the vouch.
         */
        vouch.setVoucherUser(
                buyer
        );

        /*
         * LISTER receives the vouch.
         */
        vouch.setVouchedUser(
                lister
        );

        vouch.setRequest(
                vouchRequest
        );

        vouch.setStatus(
                VouchStatus.ACTIVE
        );

        vouch.setVouchedAt(
                now
        );

        vouchRepository.saveAndFlush(
                vouch
        );

        /*
         * =========================================================
         * COMPLETE INVITATION
         * =========================================================
         */
        invitation.setStatus(
                VouchInvitationStatus.ACCEPTED
        );

        invitation.setAcceptedAt(
                now
        );

        invitation.setVouchRequest(
                vouchRequest
        );

        invitationRepository.saveAndFlush(
                invitation
        );

        /*
         * =========================================================
         * GET UPDATED ACTIVE VOUCH COUNT
         * =========================================================
         */
        long activeVouchCount =
                vouchRepository
                        .countActiveVouchesByUserId(
                                lister.getId()
                        );

        /*
         * =========================================================
         * SEND SUCCESS EMAIL TO LISTER
         * =========================================================
         *
         * IMPORTANT:
         *
         * Email failure should NOT fail or rollback
         * the actual successful vouch.
         */
        try {

            vouchSuccessEmailSender.send(
                    lister,
                    buyer,
                    activeVouchCount
            );

            auditService.log(
                    "VOUCH_SUCCESS_EMAIL_SENT",
                    lister.getId(),
                    null,
                    buyer.getId(),
                    IdentityNormalizer.maskIdentifier(
                            lister.getEmail()
                    ),
                    meta,
                    "{\"activeVouches\":"
                            + activeVouchCount
                            + "}"
            );

        } catch (Exception ex) {

            auditService.log(
                    "VOUCH_SUCCESS_EMAIL_FAILED",
                    lister.getId(),
                    null,
                    buyer.getId(),
                    IdentityNormalizer.maskIdentifier(
                            lister.getEmail()
                    ),
                    meta,
                    "{\"activeVouches\":"
                            + activeVouchCount
                            + "}"
            );
        }

        /*
         * =========================================================
         * AUDIT INVITATION COMPLETION
         * =========================================================
         */
        auditService.log(
                "VOUCH_INVITATION_ACCEPTED",
                buyer.getId(),
                null,
                lister.getId(),
                null,
                meta,
                "{\"invitationId\":\""
                        + invitation.getId()
                        + "\","
                        + "\"vouchRequestId\":\""
                        + vouchRequest.getId()
                        + "\","
                        + "\"activeVouches\":"
                        + activeVouchCount
                        + "}"
        );
    }
    /*
     * =========================================================
     * ACTIVE USER HELPER
     * =========================================================
     */
    private User activeUser(
            UUID id
    ) {

        User user =
                userRepository
                        .findById(
                                id
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User not found"
                                )
                        );

        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "USER_NOT_ACTIVE",
                    "Vouching is only available to active verified members"
            );
        }

        return user;
    }

    /*
     * =========================================================
     * RESPONSE MAPPER
     * =========================================================
     */
    private VouchRequestResponse toResponse(
            VouchRequestEntity entity
    ) {

        return new VouchRequestResponse(
                entity.getId(),
                entity.getRequester()
                        .getId(),
                entity.getTarget()
                        .getId(),
                entity.getStatus()
                        .name(),
                entity.getRequestedAt(),
                entity.getMessage()
        );
    }

    /*
     * =========================================================
     * GENERATE INVITATION TOKEN
     * =========================================================
     */
    private String generateInvitationToken() {

        byte[] bytes =
                new byte[32];

        secureRandom.nextBytes(
                bytes
        );

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        bytes
                );
    }

    /*
     * =========================================================
     * HASH INVITATION TOKEN
     * =========================================================
     */
    private String hashInvitationToken(
            String token
    ) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat
                    .of()
                    .formatHex(
                            hash
                    );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Unable to hash invitation token",
                    ex
            );
        }
    }
    @Transactional(readOnly = true)
    public List<IncomingVouchRequestResponse> getReceivedVouchRequests(
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
                                        "User not found"
                                )
                        );

        /*
         * Only BUYER receives vouch requests.
         */
        if (
                user.getAccessType()
                        != AccessTypeEnum.BUYER
        ) {

            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "BUYER_REQUIRED",
                    "Only Buyer accounts can receive vouch requests"
            );
        }

        return requestRepository
                .findByTargetIdOrderByRequestedAtDesc(
                        userId
                )
                .stream()
                .map(request ->
                        new IncomingVouchRequestResponse(
                                request.getId(),

                                request.getRequester()
                                        .getId(),

                                request.getRequester()
                                        .getFirstName(),

                                request.getRequester()
                                        .getLastName(),

                                request.getRequester()
                                        .getEmail(),

                                request.getMessage(),

                                request.getStatus()
                                        .name(),

                                request.getRequestedAt(),

                                request.getRespondedAt()
                        )
                )
                .toList();
    }

    @Transactional
    public VouchRequestResponse respondFromEmail(
            UUID requestId,
            String rawToken,
            RespondVouchRequest request,
            RequestMetadata meta
    ) {

        VouchRequestEntity entity =
                requestRepository
                        .findByIdAndStatus(
                                requestId,
                                VouchRequestStatus.PENDING
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "VOUCH_REQUEST_NOT_FOUND",
                                        "Pending vouch request not found"
                                )
                        );

        /*
         * Validate token.
         */
        if (
                rawToken == null
                        ||
                        rawToken.isBlank()
                        ||
                        entity.getResponseTokenHash() == null
        ) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_VOUCH_TOKEN",
                    "The vouch response link is invalid"
            );
        }

        /*
         * Check expiry.
         */
        if (
                entity.getResponseTokenExpiresAt() == null
                        ||
                        !entity.getResponseTokenExpiresAt()
                                .isAfter(
                                        Instant.now()
                                )
        ) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "VOUCH_TOKEN_EXPIRED",
                    "The vouch response link has expired"
            );
        }

        String suppliedHash =
                hashInvitationToken(
                        rawToken
                );

        boolean validToken =
                MessageDigest.isEqual(
                        suppliedHash.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        entity.getResponseTokenHash()
                                .getBytes(
                                        StandardCharsets.UTF_8
                                )
                );

        if (!validToken) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "INVALID_VOUCH_TOKEN",
                    "The vouch response link is invalid"
            );
        }

        /*
         * Call existing response logic.
         */
        VouchRequestResponse response =
                respond(
                        entity.getTarget()
                                .getId(),
                        requestId,
                        request,
                        meta
                );

        /*
         * Token becomes unusable after response.
         */
        entity.setResponseTokenHash(
                null
        );

        entity.setResponseTokenExpiresAt(
                null
        );

        requestRepository.save(
                entity
        );

        return response;
    }
}