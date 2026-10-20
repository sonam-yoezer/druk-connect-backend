package com.drukconnect.drukconnect.controller.dashboard;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.dashboard.ListerDashboardResponse;
import com.drukconnect.drukconnect.service.dashboard.ListerDashboardService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lister")
public class ListerDashboardController {
    private final ListerDashboardService service;

    public ListerDashboardController(ListerDashboardService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ListerDashboardResponse> dashboard(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.getDashboard(UUID.fromString(jwt.getSubject())));
    }
}
