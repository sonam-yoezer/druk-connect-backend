package com.drukconnect.drukconnect.service;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.IdentityNormalizer;
import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.dto.*;
import com.drukconnect.drukconnect.entity.*;
import com.drukconnect.drukconnect.enums.AccessTypeEnum;
import com.drukconnect.drukconnect.enums.OtpChannel;
import com.drukconnect.drukconnect.enums.UserStatus;
import com.drukconnect.drukconnect.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CommunityGuidelineAcceptanceRepository guidelineRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionRepository sessionRepository;
    private final RevokedAccessTokenRepository revokedAccessTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final AppProperties properties;

    @Autowired
    private VouchService vouchService;

    @Autowired
    private VouchRepository vouchRepository;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       CommunityGuidelineAcceptanceRepository guidelineRepository,
                       OtpService otpService,
                       PasswordEncoder passwordEncoder,
                       AuthSessionRepository sessionRepository,
                       RevokedAccessTokenRepository revokedAccessTokenRepository,
                       RefreshTokenService refreshTokenService,
                       JwtService jwtService,
                       AuditService auditService,
                       AppProperties properties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.guidelineRepository = guidelineRepository;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
        this.revokedAccessTokenRepository = revokedAccessTokenRepository;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.properties = properties;
    }

    @Transactional
    public SignupResponse signup(
            SignupRequest request,
            RequestMetadata meta
    ) {

        /*
         * =========================================================
         * NORMALIZE
         * =========================================================
         */
        String email =
                IdentityNormalizer.email(
                        request.email()
                );

        String phone =
                IdentityNormalizer.phone(
                        request.phoneNumber()
                );

        /*
         * =========================================================
         * DUPLICATE EMAIL
         * =========================================================
         */
        if (
                userRepository.existsByEmailIgnoreCase(
                        email
                )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_EXISTS",
                    "An account already exists with this email address"
            );
        }

        /*
         * =========================================================
         * DUPLICATE PHONE
         * =========================================================
         */
        if (
                userRepository.existsByPhoneNumber(
                        phone
                )
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PHONE_EXISTS",
                    "An account already exists with this phone number"
            );
        }

        /*
         * =========================================================
         * IF SIGNUP CAME FROM VOUCH INVITATION,
         * IT MUST BE A BUYER
         * =========================================================
         */
        if (
                request.vouchInvitationToken() != null
                        &&
                        !request.vouchInvitationToken().isBlank()
                        &&
                        request.accessType() != AccessTypeEnum.BUYER
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "INVITATION_REQUIRES_BUYER",
                    "A vouch invitation must be registered using a Buyer account"
            );
        }

        /*
         * =========================================================
         * CREATE USER
         * =========================================================
         */
        User user =
                new User();

        user.setFirstName(
                request.firstName()
                        .trim()
        );

        user.setLastName(
                request.lastName()
                        .trim()
        );

        user.setEmail(
                email
        );

        user.setAccessType(
                request.accessType()
        );

        user.setPhoneNumber(
                phone
        );

        user.setPasswordHash(
                passwordEncoder.encode(
                        request.password()
                )
        );

        user.setStatus(
                UserStatus.PENDING_VERIFICATION
        );

        /*
         * Save first because invitation needs
         * the generated BUYER user ID.
         */
        user =
                userRepository.saveAndFlush(
                        user
                );

        /*
         * =========================================================
         * ATTACH VOUCH INVITATION
         * =========================================================
         *
         * This connects:
         *
         * invitation.requester_user_id
         *      = original LISTER
         *
         * invitation.registered_user_id
         *      = this new BUYER
         */
        if (
                request.vouchInvitationToken() != null
                        &&
                        !request.vouchInvitationToken().isBlank()
        ) {

            vouchService.attachInvitationToBuyer(
                    request.vouchInvitationToken(),
                    user
            );
        }

        /*
         * =========================================================
         * DEFAULT ROLE
         * =========================================================
         */
        Role endUserRole =
                roleRepository
                        .findByCodeAndActiveTrue(
                                "ENDUSER"
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.INTERNAL_SERVER_ERROR,
                                        "ENDUSER_ROLE_NOT_CONFIGURED",
                                        "Default ENDUSER role is not configured"
                                )
                        );

        UserRole userRole =
                new UserRole();

        userRole.setUser(
                user
        );

        userRole.setRole(
                endUserRole
        );

        userRoleRepository.save(
                userRole
        );

        /*
         * =========================================================
         * GUIDELINE ACCEPTANCE
         * =========================================================
         */
        CommunityGuidelineAcceptance acceptance =
                new CommunityGuidelineAcceptance();

        acceptance.setUser(
                user
        );

        acceptance.setGuidelineVersion(
                properties
                        .auth()
                        .currentGuidelineVersion()
        );

        acceptance.setIpAddress(
                meta.ipAddress()
        );

        acceptance.setUserAgent(
                meta.userAgent()
        );

        guidelineRepository.save(
                acceptance
        );

        /*
         * =========================================================
         * AUDIT
         * =========================================================
         */
        auditService.log(
                "SIGNUP_CREATED",
                user.getId(),
                null,
                null,
                IdentityNormalizer.maskIdentifier(
                        email
                ),
                meta,
                request.vouchInvitationToken() != null
                        &&
                        !request.vouchInvitationToken().isBlank()
                        ? "{\"vouchInvitation\":true}"
                        : null
        );

        /*
         * =========================================================
         * PHONE OTP
         * =========================================================
         */
        if (
                properties
                        .auth()
                        .sendPhoneOtp()
        ) {

            otpService.sendSignupOtp(
                    user,
                    OtpChannel.PHONE,
                    meta,
                    false
            );
        }

        /*
         * =========================================================
         * EMAIL OTP
         * =========================================================
         */
        if (
                properties
                        .auth()
                        .sendEmailOtp()
        ) {

            otpService.sendSignupOtp(
                    user,
                    OtpChannel.EMAIL,
                    meta,
                    false
            );
        }

        return new SignupResponse(

                user.getId(),

                user.getStatus()
                        .name(),

                properties
                        .auth()
                        .requirePhoneOtp(),

                properties
                        .auth()
                        .requireEmailOtp(),

                "Account created successfully. "
                        + "Please verify the OTP sent to your email address."
        );
    }

    @Transactional(noRollbackFor = ApiException.class)
    public VerifyOtpResponse verifyOtp(
            VerifyOtpRequest request,
            RequestMetadata meta
    ) {

        /*
         * =========================================================
         * VERIFY OTP
         * =========================================================
         */
        OtpService.VerificationResult result =
                otpService.verify(
                        request.userId(),
                        request.channel(),
                        request.otp(),
                        meta
                );

        /*
         * =========================================================
         * COMPLETE VOUCH INVITATION
         * =========================================================
         *
         * Only after the account becomes ACTIVE.
         *
         * If this BUYER registered through a vouch invitation,
         * create the actual ACTIVE vouch for the original LISTER.
         */
        if (
                result.accountActive()
        ) {

            User user =
                    userRepository
                            .findById(
                                    request.userId()
                            )
                            .orElseThrow(() ->
                                    new ApiException(
                                            HttpStatus.NOT_FOUND,
                                            "USER_NOT_FOUND",
                                            "User not found"
                                    )
                            );

            if (
                    user.getAccessType()
                            == AccessTypeEnum.BUYER
            ) {

                vouchService.completeInvitationForBuyer(
                        user,
                        meta
                );
            }
        }

        /*
         * =========================================================
         * RESPONSE
         * =========================================================
         */
        return new VerifyOtpResponse(
                true,
                result.phoneVerified(),
                result.emailVerified(),
                result.accountActive(),
                result.accountActive()
                        ? "Verification complete. You can now log in."
                        : "OTP verified. Complete the remaining verification."
        );
    }

    @Transactional
    public MessageResponse resendOtp(
            ResendOtpRequest request,
            RequestMetadata meta
    ) {

        User user =
                userRepository
                        .findById(
                                request.userId()
                        )
                        .orElseThrow(() ->
                                new ApiException(
                                        HttpStatus.NOT_FOUND,
                                        "USER_NOT_FOUND",
                                        "User not found"
                                )
                        );


        /*
         * =========================================================
         * PHONE OTP CURRENTLY DISABLED
         * =========================================================
         */
        if (
                request.channel()
                        == OtpChannel.PHONE

                        &&

                        !properties
                                .auth()
                                .sendPhoneOtp()
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PHONE_OTP_DISABLED",
                    "Phone OTP verification is currently disabled"
            );
        }


        /*
         * =========================================================
         * EMAIL OTP
         * =========================================================
         */
        if (
                request.channel()
                        == OtpChannel.EMAIL

                        &&

                        !properties
                                .auth()
                                .sendEmailOtp()
        ) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "EMAIL_OTP_DISABLED",
                    "Email OTP verification is currently disabled"
            );
        }


        if (
                request.channel()
                        == OtpChannel.PHONE

                        &&

                        user.getPhoneVerifiedAt()
                                != null
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PHONE_ALREADY_VERIFIED",
                    "Phone number is already verified"
            );
        }


        if (
                request.channel()
                        == OtpChannel.EMAIL

                        &&

                        user.getEmailVerifiedAt()
                                != null
        ) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "EMAIL_ALREADY_VERIFIED",
                    "Email address is already verified"
            );
        }


        otpService.sendSignupOtp(
                user,
                request.channel(),
                meta,
                true
        );


        auditService.log(
                "OTP_RESENT",
                user.getId(),
                null,
                null,
                null,
                meta,
                "{\"channel\":\""
                        + request.channel()
                        + "\"}"
        );


        return new MessageResponse(
                request.channel()
                        == OtpChannel.EMAIL
                        ? "A new OTP has been sent to your email address."
                        : "A new OTP has been sent to your phone number."
        );
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse login(
            LoginRequest request,
            RequestMetadata meta
    ) {

        String rawIdentifier =
                request.identifier().trim();

        User user;

        String normalized;

        /*
         * =========================================================
         * FIND USER
         * =========================================================
         */
        if (rawIdentifier.contains("@")) {

            normalized =
                    IdentityNormalizer.email(
                            rawIdentifier
                    );

            user =
                    userRepository
                            .findByEmailIgnoreCase(
                                    normalized
                            )
                            .orElse(null);

        } else {

            normalized =
                    IdentityNormalizer.phone(
                            rawIdentifier
                    );

            user =
                    userRepository
                            .findByPhoneNumber(
                                    normalized
                            )
                            .orElse(null);
        }

        /*
         * =========================================================
         * VERIFY PASSWORD
         * =========================================================
         */
        if (
                user == null
                        ||
                        !passwordEncoder.matches(
                                request.password(),
                                user.getPasswordHash()
                        )
        ) {

            if (user != null) {

                user.setFailedLoginAttempts(
                        user.getFailedLoginAttempts() + 1
                );

                userRepository.save(user);
            }

            auditService.log(
                    "LOGIN_FAILED",
                    user == null
                            ? null
                            : user.getId(),
                    null,
                    null,
                    IdentityNormalizer.maskIdentifier(
                            normalized
                    ),
                    meta,
                    "{\"reason\":\"BAD_CREDENTIALS\"}"
            );

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "BAD_CREDENTIALS",
                    "Invalid email/phone number or password"
            );
        }

        /*
         * =========================================================
         * EMAIL VERIFICATION
         * =========================================================
         */
        if (
                properties.auth().requireEmailOtp()
                        &&
                        user.getEmailVerifiedAt() == null
        ) {

            throw loginBlocked(
                    user,
                    normalized,
                    meta,
                    "EMAIL_NOT_VERIFIED",
                    "Verify your email address before logging in"
            );
        }

        /*
         * =========================================================
         * ACCOUNT STATUS
         * =========================================================
         */
        if (
                user.getStatus()
                        != UserStatus.ACTIVE
        ) {

            throw loginBlocked(
                    user,
                    normalized,
                    meta,
                    "ACCOUNT_NOT_ACTIVE",
                    "This account is not active"
            );
        }

        /*
         * =========================================================
         * ACCESS TYPE VALIDATION
         * =========================================================
         *
         * BUYER:
         * No vouch required.
         *
         * LISTER:
         * Minimum 2 ACTIVE vouches required.
         * =========================================================
         */
        if (
                user.getAccessType()
                        == AccessTypeEnum.LISTER
        ) {

            long activeVouchCount =
                    vouchRepository
                            .countActiveVouchesByUserId(
                                    user.getId()
                            );

            /*
             * Lister requires minimum 2 vouches.
             */
            if (activeVouchCount < 2) {

                auditService.log(
                        "LOGIN_BLOCKED_INSUFFICIENT_VOUCHES",
                        user.getId(),
                        null,
                        null,
                        IdentityNormalizer.maskIdentifier(
                                normalized
                        ),
                        meta,
                        "{\"accessType\":\"LISTER\","
                                + "\"activeVouches\":"
                                + activeVouchCount
                                + ",\"requiredVouches\":2}"
                );

                throw new ApiException(
                        HttpStatus.FORBIDDEN,
                        "INSUFFICIENT_VOUCHES",
                        "Lister accounts require at least 2 active vouches before logging in. "
                                + "Current active vouches: "
                                + activeVouchCount
                );
            }
        }

        /*
         * =========================================================
         * LOGIN SUCCESS
         * =========================================================
         */
        user.setFailedLoginAttempts(
                0
        );

        user.setLastLoginAt(
                Instant.now()
        );

        userRepository.save(user);

        /*
         * =========================================================
         * LOAD SECURITY ROLES
         * =========================================================
         */
        List<String> roles =
                userRoleRepository
                        .findRoleCodesByUserId(
                                user.getId()
                        );

        /*
         * =========================================================
         * CREATE SESSION + TOKENS
         * =========================================================
         */
        TokenResponse response =
                createSessionAndTokens(
                        user,
                        roles,
                        meta
                );

        UUID sessionId =
                sessionRepository
                        .findByRefreshTokenHashAndRevokedAtIsNull(
                                refreshTokenService.hash(
                                        response.refreshToken()
                                )
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "New authentication session was not persisted"
                                )
                        )
                        .getId();

        /*
         * =========================================================
         * AUDIT LOGIN SUCCESS
         * =========================================================
         */
        auditService.log(
                "LOGIN_SUCCESS",
                user.getId(),
                sessionId,
                null,
                IdentityNormalizer.maskIdentifier(
                        normalized
                ),
                meta,
                "{\"accessType\":\""
                        + user.getAccessType()
                        + "\"}"
        );

        return response;
    }

    @Transactional(noRollbackFor = ApiException.class)
    public TokenResponse refresh(RefreshTokenRequest request, RequestMetadata meta) {
        String tokenHash = refreshTokenService.hash(request.refreshToken());
        AuthSession session = sessionRepository.findByRefreshTokenHashAndRevokedAtIsNull(tokenHash)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "Refresh token is invalid"));

        Instant now = Instant.now();
        if (!session.getRefreshExpiresAt().isAfter(now)) {
            session.setRevokedAt(now);
            session.setRevocationReason("REFRESH_EXPIRED");
            sessionRepository.save(session);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh token has expired. Please log in again");
        }

        User user = session.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_NOT_ACTIVE", "Account is not active");
        }

        revokeCurrentAccessIfNeeded(session, "REFRESH_ROTATION");

        List<String> roles = userRoleRepository.findRoleCodesByUserId(user.getId());
        String newRefresh = refreshTokenService.generate();
        // Absolute 7-day session lifetime: rotating the refresh token does not extend the original expiry.
        Instant refreshExpiresAt = session.getRefreshExpiresAt();
        JwtService.AccessToken access = jwtService.issueAccessToken(user, session.getId(), roles);

        session.setRefreshTokenHash(refreshTokenService.hash(newRefresh));
        session.setRefreshExpiresAt(refreshExpiresAt);
        session.setCurrentAccessJti(access.jti());
        session.setAccessExpiresAt(access.expiresAt());
        session.setLastUsedAt(now);
        session.setIpAddress(meta.ipAddress());
        session.setUserAgent(meta.userAgent());
        sessionRepository.save(session);

        auditService.log("TOKEN_REFRESHED", user.getId(), session.getId(), null, null, meta, null);
        return tokenResponse(user, roles, access, newRefresh, refreshExpiresAt);
    }

    @Transactional
    public MessageResponse logout(Jwt jwt, RequestMetadata meta) {
        UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        UUID userId = UUID.fromString(jwt.getSubject());
        AuthSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "SESSION_NOT_FOUND", "Session not found"));

        Instant now = Instant.now();
        if (session.getRevokedAt() == null) {
            session.setRevokedAt(now);
            session.setRevocationReason("USER_LOGOUT");
            session.setLastUsedAt(now);
            sessionRepository.save(session);
        }

        if (!revokedAccessTokenRepository.existsByJti(jwt.getId())) {
            RevokedAccessToken revoked = new RevokedAccessToken();
            revoked.setJti(jwt.getId());
            revoked.setSessionId(sessionId);
            revoked.setUserId(userId);
            revoked.setExpiresAt(jwt.getExpiresAt());
            revoked.setReason("USER_LOGOUT");
            revokedAccessTokenRepository.save(revoked);
        }

        auditService.log("LOGOUT", userId, sessionId, null, null, meta, null);
        return new MessageResponse("Logged out successfully.");
    }

    private ApiException loginBlocked(User user, String identifier, RequestMetadata meta, String reason, String message) {
        auditService.log("LOGIN_BLOCKED", user.getId(), null, null,
                IdentityNormalizer.maskIdentifier(identifier), meta, "{\"reason\":\"" + reason + "\"}");
        return new ApiException(HttpStatus.FORBIDDEN, reason, message);
    }

    private TokenResponse createSessionAndTokens(User user, List<String> roles, RequestMetadata meta) {
        Instant now = Instant.now();
        String refresh = refreshTokenService.generate();
        Instant refreshExpiresAt = now.plus(properties.jwt().refreshTtl());

        AuthSession session = new AuthSession();
        session.setUser(user);
        session.setRefreshTokenHash(refreshTokenService.hash(refresh));
        session.setCurrentAccessJti(UUID.randomUUID().toString()); // temporary inside this transaction
        session.setAccessExpiresAt(now);
        session.setRefreshExpiresAt(refreshExpiresAt);
        session.setIpAddress(meta.ipAddress());
        session.setUserAgent(meta.userAgent());
        session = sessionRepository.saveAndFlush(session);

        JwtService.AccessToken access = jwtService.issueAccessToken(user, session.getId(), roles);
        session.setCurrentAccessJti(access.jti());
        session.setAccessExpiresAt(access.expiresAt());
        sessionRepository.save(session);

        return tokenResponse(user, roles, access, refresh, refreshExpiresAt);
    }

    private TokenResponse tokenResponse(User user,
                                        List<String> roles,
                                        JwtService.AccessToken access,
                                        String refresh,
                                        Instant refreshExpiresAt) {
        UserSummary summary = new UserSummary(
                user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber(), roles
        );
        return new TokenResponse(
                "Bearer",
                access.value(),
                refresh,
                properties.jwt().accessTtl().toSeconds(),
                Math.max(0, java.time.Duration.between(Instant.now(), refreshExpiresAt).toSeconds()),
                access.expiresAt(),
                refreshExpiresAt,
                summary
        );
    }

    private void revokeCurrentAccessIfNeeded(AuthSession session, String reason) {
        if (session.getCurrentAccessJti() == null || session.getAccessExpiresAt() == null) return;
        if (!session.getAccessExpiresAt().isAfter(Instant.now())) return;
        if (revokedAccessTokenRepository.existsByJti(session.getCurrentAccessJti())) return;

        RevokedAccessToken revoked = new RevokedAccessToken();
        revoked.setJti(session.getCurrentAccessJti());
        revoked.setSessionId(session.getId());
        revoked.setUserId(session.getUser().getId());
        revoked.setExpiresAt(session.getAccessExpiresAt());
        revoked.setReason(reason);
        revokedAccessTokenRepository.save(revoked);
    }

}
