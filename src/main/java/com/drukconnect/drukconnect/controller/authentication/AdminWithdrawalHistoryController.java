package com.drukconnect.drukconnect.controller.authentication;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.authentication.AdminWithdrawalHistoryResponses.*;
import com.drukconnect.drukconnect.service.authentication.AdminWithdrawalHistoryService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/vouch-withdrawals")
public class AdminWithdrawalHistoryController {
    private final AdminWithdrawalHistoryService service;
    public AdminWithdrawalHistoryController(AdminWithdrawalHistoryService service) { this.service = service; }
    @GetMapping
    public ResponseEntity<Results> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return privateResponse(service.list(userId(jwt), status, page, size));
    }
    @GetMapping("/listers/{listerId}/history")
    public ResponseEntity<History> history(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listerId,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "5") int size) {
        return privateResponse(service.history(userId(jwt), listerId, status, page, size));
    }
    private UUID userId(Jwt jwt) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
    private <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
