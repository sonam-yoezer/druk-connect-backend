package com.drukconnect.drukconnect.repository;

import com.drukconnect.drukconnect.entity.AuthAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AuthAuditLogRepository extends JpaRepository<AuthAuditLog, UUID> {}
