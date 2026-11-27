package com.drukconnect.drukconnect.service.moderation;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.moderation.*;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import com.drukconnect.drukconnect.enums.moderation.ScamReportStatus;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.repository.moderation.ListerScamReportRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AdminScamReporterHistoryService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final ListerScamReportRepository reports;
    private final ListerScamModerationService moderation;
    public AdminScamReporterHistoryService(UserRepository users, UserRoleRepository roles,
            ListerScamReportRepository reports, ListerScamModerationService moderation) {
        this.users = users; this.roles = roles; this.reports = reports; this.moderation = moderation;
    }
    @Transactional(readOnly = true)
    public ScamReporterHistoryResponse get(UUID adminId, UUID reporterId, ScamReportStatus status, int page, int size) {
        requireAdmin(adminId);
        if (page < 1 || size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_PAGINATION", "Page must be at least 1 and size must be between 1 and 50");
        var reporter = users.findById(reporterId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "REPORTER_NOT_FOUND", "Reporter not found"));
        var stats = reports.getReporterStats(reporterId);
        var pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("reportedAt"), Sort.Order.desc("id")));
        var result = status == null ? reports.findAllByReporterId(reporterId, pageable)
                : reports.findAllByReporterIdAndStatus(reporterId, status, pageable);
        var history = new PagedListerScamReportResponse(result.getContent().stream()
                .map(report -> moderation.toReportResponse(report, null, stats.getTotalReports())).toList(),
                result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages(),
                result.hasNext(), result.hasPrevious());
        return new ScamReporterHistoryResponse(reporterId,
                (reporter.getFirstName() + " " + reporter.getLastName()).trim(), stats.getTotalReports(),
                stats.getPendingReports(), stats.getApprovedReports(), stats.getRejectedReports(),
                stats.getFirstReportedAt(), stats.getLastReportedAt(), history);
    }
    @Transactional(readOnly = true)
    public void requireAdmin(UUID adminId) {
        var admin = users.findById(adminId).orElse(null);
        if (admin == null || admin.getStatus() != UserStatus.ACTIVE
                || roles.findRoleCodesByUserId(adminId).stream().noneMatch("ADMIN"::equalsIgnoreCase)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "An active Admin account is required to view reporter history");
        }
    }

}
