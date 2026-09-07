package com.drukconnect.drukconnect.security;

import com.drukconnect.drukconnect.entity.AuthSession;
import com.drukconnect.drukconnect.enums.UserStatus;
import com.drukconnect.drukconnect.repository.AuthSessionRepository;
import com.drukconnect.drukconnect.repository.RevokedAccessTokenRepository;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private final AuthSessionRepository sessionRepository;
    private final RevokedAccessTokenRepository revokedRepository;

    public AccessTokenValidator(AuthSessionRepository sessionRepository,
                                RevokedAccessTokenRepository revokedRepository) {
        this.sessionRepository = sessionRepository;
        this.revokedRepository = revokedRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            if (!"access".equals(jwt.getClaimAsString("type"))) return fail("Wrong token type");
            if (jwt.getId() == null || revokedRepository.existsByJti(jwt.getId())) return fail("Token has been revoked");

            String sid = jwt.getClaimAsString("sid");
            if (sid == null) return fail("Missing session id");

            AuthSession session = sessionRepository.findByIdAndRevokedAtIsNull(UUID.fromString(sid)).orElse(null);
            if (session == null) return fail("Session is not active");
            if (!session.getCurrentAccessJti().equals(jwt.getId())) return fail("Token is no longer current");
            if (!session.getUser().getId().toString().equals(jwt.getSubject())) return fail("Session/user mismatch");
            if (session.getUser().getStatus() != UserStatus.ACTIVE) return fail("Account is not active");

            return OAuth2TokenValidatorResult.success();
        } catch (Exception ex) {
            return fail("Invalid access token");
        }
    }

    private OAuth2TokenValidatorResult fail(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }
}
