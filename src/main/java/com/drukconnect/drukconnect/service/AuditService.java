package com.drukconnect.drukconnect.service;

import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.entity.AuthAuditLog;
import com.drukconnect.drukconnect.repository.AuthAuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {
    private final AuthAuditLogRepository repository;

    public AuditService(AuthAuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String eventType,
                    UUID userId,
                    UUID sessionId,
                    UUID targetUserId,
                    String identifierMasked,
                    RequestMetadata meta,
                    String metadataJson) {
        AuthAuditLog log = new AuthAuditLog();
        log.setEventType(eventType);
        log.setUserId(userId);
        log.setSessionId(sessionId);
        log.setTargetUserId(targetUserId);
        log.setIdentifierMasked(identifierMasked);
        if (meta != null) {
            log.setIpAddress(meta.ipAddress());
            log.setUserAgent(meta.userAgent());
        }
        log.setMetadataJson(metadataJson);
        repository.save(log);
    }
}
