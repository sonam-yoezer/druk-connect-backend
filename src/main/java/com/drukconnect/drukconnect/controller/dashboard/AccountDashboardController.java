package com.drukconnect.drukconnect.controller.dashboard;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.dashboard.*;
import com.drukconnect.drukconnect.service.dashboard.AccountDashboardService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class AccountDashboardController {
    private final AccountDashboardService service;
    public AccountDashboardController(AccountDashboardService service) { this.service = service; }

    @GetMapping("/buyer/dashboard")
    public ResponseEntity<BuyerDashboardResponse> buyer(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.buyer(userId(jwt)));
    }
    @GetMapping("/admin/dashboard")
    public ResponseEntity<AdminDashboardResponse> admin(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.admin(userId(jwt)));
    }
    private UUID userId(Jwt jwt) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
}
