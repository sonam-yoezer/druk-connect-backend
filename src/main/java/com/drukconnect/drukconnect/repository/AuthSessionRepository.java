package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.AuthSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuthSession> findByRefreshTokenHashAndRevokedAtIsNull(String refreshTokenHash);
    Optional<AuthSession> findByIdAndRevokedAtIsNull(UUID id);
}
