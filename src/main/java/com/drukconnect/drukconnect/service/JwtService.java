package com.drukconnect.drukconnect.service;

import com.drukconnect.drukconnect.config.AppProperties;
import com.drukconnect.drukconnect.entity.User;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtEncoder encoder;
    private final AppProperties properties;

    public JwtService(JwtEncoder encoder, AppProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public AccessToken issueAccessToken(User user, UUID sessionId, List<String> roles) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.jwt().accessTtl());
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(jti)
                .claim("sid", sessionId.toString())
                .claim("type", "access")
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        Jwt jwt = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new AccessToken(jwt.getTokenValue(), jti, expiresAt);
    }

    public record AccessToken(String value, String jti, Instant expiresAt) {}
}
