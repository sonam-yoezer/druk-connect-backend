package com.drukconnect.drukconnect.controller.profile;
import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.profile.*;
import com.drukconnect.drukconnect.service.profile.ProfileContactChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/me/contact-changes")
public class ProfileContactChangeController {
    private final ProfileContactChangeService service;
    public ProfileContactChangeController(ProfileContactChangeService service) { this.service = service; }
    @PostMapping
    public ContactChangeResponse start(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartContactChangeRequest body, HttpServletRequest request) {
        return service.start(userId(jwt), body, RequestMetadata.from(request));
    }
    @PostMapping("/verify")
    public ProfileResponse verify(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody VerifyContactChangeRequest body, HttpServletRequest request) {
        return service.verify(userId(jwt), body, RequestMetadata.from(request));
    }
    private UUID userId(Jwt jwt) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
}
