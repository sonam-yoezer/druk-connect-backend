package com.drukconnect.drukconnect.controller.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.common.RequestMetadata;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.service.listing.ListingReviewRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ListingReviewRequestController {
    private final ListingReviewRequestService service;
    public ListingReviewRequestController(ListingReviewRequestService service) { this.service = service; }

    @PostMapping("/listings/{listingId}/review-contact")
    public ReviewEligibilityResponse contact(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId,
            HttpServletRequest request) {
        return service.recordWhatsappContact(userId(jwt), listingId, RequestMetadata.from(request));
    }
    @GetMapping("/listings/{listingId}/review-eligibility")
    public ReviewEligibilityResponse eligibility(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId) {
        return service.eligibility(userId(jwt), listingId);
    }
    @GetMapping("/listings/{listingId}/review-requests/buyers/search")
    public PagedReviewRecipientsResponse search(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId,
            @RequestParam String query, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.search(userId(jwt), listingId, query, page, size);
    }
    @PostMapping("/listings/{listingId}/review-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewInvitationResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId,
            @Valid @RequestBody CreateReviewInvitationRequest body, HttpServletRequest request) {
        return service.requestReview(userId(jwt), listingId, body.buyerUserId(), RequestMetadata.from(request));
    }
    @GetMapping("/listings/{listingId}/review-requests")
    public PagedReviewInvitationsResponse list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID listingId,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return service.list(userId(jwt), listingId, page, size);
    }
    @GetMapping("/review-requests/{requestId}")
    public ReviewInvitationResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID requestId) {
        return service.getForBuyer(userId(jwt), requestId);
    }
    private UUID userId(Jwt jwt) {
        if (jwt == null) throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
}
