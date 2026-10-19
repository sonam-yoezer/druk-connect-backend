package com.drukconnect.drukconnect.service.listing;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.listing.CreateListingReviewRequest;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.listing.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import com.drukconnect.drukconnect.util.ReviewRequestEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.util.Set;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:review_request_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa",
    "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.mail.enabled=false", "app.sms.enabled=false"
})
class ReviewRequestFlowIntegrationTest {
    @Autowired ListingReviewRequestService service;
    @Autowired ListingServiceAvailmentService availments;
    @Autowired ListingReviewService reviews;
    @Autowired UserRepository users;
    @Autowired ListingRepository listings;
    @Autowired ListingReviewRequestRepository requests;
    @Autowired ListingReviewContactRepository contacts;
    @Autowired WebApplicationContext context;
    @MockitoBean ReviewRequestEmailSender email;
    @MockitoBean AuditService audit;
    User lister, buyer;
    Listing listing;
    MockMvc mvc;

    @BeforeEach void setup() {
        lister = users.saveAndFlush(user(AccessTypeEnum.LISTER));
        buyer = users.saveAndFlush(user(AccessTypeEnum.BUYER));
        listing = new Listing(); listing.setLister(lister); listing.setListingTitle("Home Tour");
        listing.setListingCategory("Tours"); listing.setDescription("Local tour"); listing.setServiceType("Tour");
        listing.setAvailability("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY"); listing.setPricingType(ListingPricingType.FREE);
        listing.setSuburb("Thimphu"); listing.setState("Thimphu"); listing.setPostcode("11001");
        listing = listings.saveAndFlush(listing);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    User user(AccessTypeEnum type) {
        User user = new User(); user.setFirstName("Test"); user.setLastName("Buyer");
        user.setEmail(UUID.randomUUID() + "@example.com"); user.setPhoneNumber(UUID.randomUUID().toString().substring(0, 18));
        user.setPasswordHash("test-only"); user.setStatus(UserStatus.ACTIVE); user.setAccessType(type); return user;
    }
    @Test void invitationAllowsReviewAndSubmissionCompletesIt() {
        assertFalse(service.eligibility(buyer.getId(), listing.getId()).canReview());
        var invitation = service.requestReview(lister.getId(), listing.getId(), buyer.getId(), null);
        assertTrue(service.eligibility(buyer.getId(), listing.getId()).canReview());
        var review = reviews.createReview(buyer.getId(), listing.getId(), new CreateListingReviewRequest(5, "Great tour", Set.of("Friendly")), null);
        assertEquals("PENDING", review.status());
        assertEquals("COMPLETED", requests.findById(invitation.requestId()).orElseThrow().getStatus());
        assertFalse(service.eligibility(buyer.getId(), listing.getId()).canReview());
        assertThrows(ApiException.class, () -> reviews.createReview(buyer.getId(), listing.getId(), new CreateListingReviewRequest(4, "Again", Set.of()), null));
        verify(email).send(any(), any(), any(), contains("/buyer-dashboard/services/" + listing.getId()));
    }
    @Test void mailFailureRollsBackInvitationAndEligibility() {
        doThrow(new IllegalStateException("SMTP down")).when(email).send(any(), any(), any(), anyString());
        assertThrows(ApiException.class, () -> service.requestReview(lister.getId(), listing.getId(), buyer.getId(), null));
        assertTrue(requests.findByListingIdAndBuyerId(listing.getId(), buyer.getId()).isEmpty());
        assertFalse(service.eligibility(buyer.getId(), listing.getId()).canReview());
    }
    @Test void whatsappContactIsIdempotentAndAllowsReview() {
        assertTrue(service.recordWhatsappContact(buyer.getId(), listing.getId(), null).canReview());
        service.recordWhatsappContact(buyer.getId(), listing.getId(), null);
        assertTrue(contacts.existsByListingIdAndBuyerId(listing.getId(), buyer.getId()));
        reviews.createReview(buyer.getId(), listing.getId(), new CreateListingReviewRequest(4, "Good", Set.of()), null);
    }
    @Test void sensitiveListingReadsRequireAuthentication() throws Exception {
        for (String suffix : new String[]{"review-eligibility", "review-requests", "review-requests/buyers/search?query=Test"})
            mvc.perform(get("/api/v1/listings/" + listing.getId() + "/" + suffix)).andExpect(status().isUnauthorized());
    }
    @Test void searchIsOwnerOnlyAndMatchesPhoneEmailAndFullName() throws Exception {
        for (String query : new String[]{buyer.getPhoneNumber(), buyer.getEmail(), "Test Buyer"}) {
            var result = service.search(lister.getId(), listing.getId(), query, 1, 50);
            assertTrue(result.buyers().stream().anyMatch(user -> user.userId().equals(buyer.getId())));
        }
        mvc.perform(get("/api/v1/listings/" + listing.getId() + "/review-requests/buyers/search")
            .param("query", "Test").with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString())))).andExpect(status().isForbidden());
    }
    @Test void buyerCannotReadAnotherRecipientsInvitation() throws Exception {
        var invitation = service.requestReview(lister.getId(), listing.getId(), buyer.getId(), null);
        User stranger = users.saveAndFlush(user(AccessTypeEnum.BUYER));
        mvc.perform(get("/api/v1/review-requests/" + invitation.requestId())
            .with(jwt().jwt(jwt -> jwt.subject(stranger.getId().toString())))).andExpect(status().isForbidden());
    }
    @Test void markAvailedAppearsInBothHistoriesWithoutSendingEmail() {
        var record = availments.mark(lister.getId(), listing.getId(), buyer.getId(), null);
        assertTrue(availments.history(lister.getId(), 1, 10).availments().stream()
            .anyMatch(row -> row.availmentId().equals(record.availmentId())));
        assertTrue(availments.history(buyer.getId(), 1, 10).availments().stream()
            .anyMatch(row -> row.availmentId().equals(record.availmentId())));
        verifyNoInteractions(email);
        assertTrue(service.eligibility(buyer.getId(), listing.getId()).canReview());
        reviews.createReview(buyer.getId(), listing.getId(), new CreateListingReviewRequest(5, "Service availed", Set.of()), null);
        assertTrue(availments.history(buyer.getId(), 1, 10).availments().getFirst().alreadyReviewed());
    }
    @Test void duplicateAvailmentPreservesOriginalHistoryAndDate() {
        var first = availments.mark(lister.getId(), listing.getId(), buyer.getId(), null);
        var again = availments.mark(lister.getId(), listing.getId(), buyer.getId(), null);
        assertEquals(first.availmentId(), again.availmentId());
        // Database DATETIME(6) may round the last nanoseconds.
        assertEquals(first.availedAt().getEpochSecond(), again.availedAt().getEpochSecond());
        assertEquals(1, availments.history(buyer.getId(), 1, 10).totalElements());
    }
    @Test void availmentHistoryIsPrivateAndOnlyOwnerCanMark() {
        availments.mark(lister.getId(), listing.getId(), buyer.getId(), null);
        User otherBuyer = users.saveAndFlush(user(AccessTypeEnum.BUYER));
        User otherLister = users.saveAndFlush(user(AccessTypeEnum.LISTER));
        assertTrue(availments.history(otherBuyer.getId(), 1, 10).availments().isEmpty());
        assertTrue(availments.history(otherLister.getId(), 1, 10).availments().isEmpty());
        assertThrows(ApiException.class, () -> availments.mark(otherLister.getId(), listing.getId(), buyer.getId(), null));
        assertThrows(ApiException.class, () -> availments.mark(buyer.getId(), listing.getId(), otherBuyer.getId(), null));
    }
    @Test void historyRequiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/service-availments/me")).andExpect(status().isUnauthorized());
    }

}
