package com.drukconnect.drukconnect.dto.moderation;

import java.time.Instant;
import java.util.UUID;

public record ScamReporterHistoryResponse(UUID reporterUserId, String reporterName,
        long totalReports, long pendingReports, long approvedReports, long rejectedReports,
        Instant firstReportedAt, Instant lastReportedAt, PagedListerScamReportResponse history) {}
