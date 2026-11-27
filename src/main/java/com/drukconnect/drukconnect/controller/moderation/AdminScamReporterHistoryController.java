package com.drukconnect.drukconnect.controller.moderation;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.moderation.ScamReporterHistoryResponse;
import com.drukconnect.drukconnect.enums.moderation.ScamReportStatus;
import com.drukconnect.drukconnect.service.moderation.AdminScamReporterHistoryService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/lister-scam/reporters")
public class AdminScamReporterHistoryController {
    private final AdminScamReporterHistoryService service;
    public AdminScamReporterHistoryController(AdminScamReporterHistoryService service) { this.service = service; }
    @GetMapping("/{reporterId}/history")
    public ResponseEntity<ScamReporterHistoryResponse> history(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID reporterId, @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(
                UUID.fromString(jwt.getSubject()), reporterId, parseStatus(status), page, size));
    }
    private ScamReportStatus parseStatus(String status) {
        if (status == null) return null;
        try { return ScamReportStatus.valueOf(status); }
        catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_REPORT_STATUS",
                    "Report status must be PENDING, APPROVED, or REJECTED");
        }
    }
}
