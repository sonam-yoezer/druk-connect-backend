package com.drukconnect.drukconnect.controller.listing;
import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.listing.*;
import com.drukconnect.drukconnect.service.listing.ListingServiceAvailmentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ListingServiceAvailmentController {
    private final ListingServiceAvailmentService service;
    public ListingServiceAvailmentController(ListingServiceAvailmentService service) {this.service=service;}
    @PostMapping("/listings/{listingId}/availments")
    public ServiceAvailmentResponse mark(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID listingId,
            @Valid @RequestBody CreateReviewInvitationRequest body,HttpServletRequest request) {
        return service.mark(userId(jwt),listingId,body.buyerUserId(),RequestMetadata.from(request));
    }
    @GetMapping("/service-availments/me")
    public PagedServiceAvailmentsResponse history(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="10") int size) {
        return service.history(userId(jwt),page,size);
    }
    private UUID userId(Jwt jwt) {
        if(jwt==null) throw new ApiException(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Sign in to continue");
        return UUID.fromString(jwt.getSubject());
    }
}
