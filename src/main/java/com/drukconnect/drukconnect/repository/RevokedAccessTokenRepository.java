package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.RevokedAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RevokedAccessTokenRepository extends JpaRepository<RevokedAccessToken, UUID> {
    boolean existsByJti(String jti);
}
