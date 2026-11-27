package com.drukconnect.drukconnect.controller.authentication;

import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.authentication.BuyerOfferedVouchResponses.*;
import com.drukconnect.drukconnect.service.authentication.BuyerOfferedVouchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buyer/vouches")
public class BuyerOfferedVouchController {
    private final BuyerOfferedVouchService service;
    public BuyerOfferedVouchController(BuyerOfferedVouchService service) { this.service = service; }
    @GetMapping("/listers/search")
    public ResponseEntity<List<Candidate>> search(@AuthenticationPrincipal Jwt jwt, @RequestParam String query) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.search(id(jwt),query));
    }
    @PostMapping("/listers/{listerId}")
    public Result give(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listerId, HttpServletRequest request) {
        return service.give(id(jwt),listerId,RequestMetadata.from(request));
    }
    private UUID id(Jwt jwt) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
}
