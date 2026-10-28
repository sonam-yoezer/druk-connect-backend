package com.drukconnect.drukconnect.controller.profile;

import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.profile.*;
import com.drukconnect.drukconnect.service.profile.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {
    private final UserProfileService service;
    public UserProfileController(UserProfileService service) { this.service = service; }
    @GetMapping("/me/profile")
    public ResponseEntity<ProfileResponse> profile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(userId(jwt)));
    }
    @PatchMapping("/me/profile")
    public ProfileResponse update(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest body, HttpServletRequest request) {
        return service.update(userId(jwt), body, RequestMetadata.from(request));
    }
    @PutMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProfileResponse upload(@AuthenticationPrincipal Jwt jwt, @RequestParam("image") MultipartFile image, HttpServletRequest request) {
        return service.upload(userId(jwt), image, RequestMetadata.from(request));
    }
    @DeleteMapping("/me/profile-image")
    public ProfileResponse remove(@AuthenticationPrincipal Jwt jwt, HttpServletRequest request) {
        return service.removeImage(userId(jwt), RequestMetadata.from(request));
    }
    @GetMapping("/{userId}/profile-image")
    public ResponseEntity<Resource> image(@PathVariable UUID userId) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff").body(service.image(userId));
    }
    private UUID userId(Jwt jwt) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
}
