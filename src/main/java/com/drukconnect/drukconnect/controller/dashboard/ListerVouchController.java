package com.drukconnect.drukconnect.controller.dashboard;

import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.authentication.*;
import com.drukconnect.drukconnect.dto.dashboard.ListerVouchResponses.*;
import com.drukconnect.drukconnect.service.authentication.VouchService;
import com.drukconnect.drukconnect.service.dashboard.ListerVouchService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/lister/vouches")
public class ListerVouchController {
    private final ListerVouchService service;
    private final VouchService requests;
    public ListerVouchController(ListerVouchService service, VouchService requests) {
        this.service = service; this.requests = requests;
    }
    @GetMapping
    public ResponseEntity<Results<Received>> received(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page) {
        return privateResponse(service.received(userId(jwt), page));
    }
    @GetMapping("/requests")
    public ResponseEntity<Results<Sent>> sent(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page) {
        return privateResponse(service.sent(userId(jwt), page));
    }
    @GetMapping("/members/search")
    public ResponseEntity<List<Candidate>> search(@AuthenticationPrincipal Jwt jwt, @RequestParam String query) {
        return privateResponse(service.search(userId(jwt), query));
    }
    @GetMapping("/members/{memberId}")
    public ResponseEntity<Member> profile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID memberId) {
        return privateResponse(service.profile(userId(jwt), memberId));
    }
    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public VouchRequestResponse create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateVouchRequest body, HttpServletRequest request) {
        return requests.create(userId(jwt), body, RequestMetadata.from(request));
    }
    private <T> ResponseEntity<T> privateResponse(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
    private UUID userId(Jwt jwt) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
}
