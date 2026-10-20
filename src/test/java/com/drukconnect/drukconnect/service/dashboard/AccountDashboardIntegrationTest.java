package com.drukconnect.drukconnect.service.dashboard;

import com.drukconnect.drukconnect.entity.authentication.*;
import com.drukconnect.drukconnect.entity.listing.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.repository.listing.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:account_dashboard_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.mail.enabled=false", "app.sms.enabled=false"
})
@Transactional
class AccountDashboardIntegrationTest {
    @Autowired AccountDashboardService service;
    @Autowired UserRepository users;
    @Autowired UserRoleRepository userRoles;
    @Autowired RoleRepository roles;
    @Autowired ListingRepository listings;
    @Autowired ListingReviewRepository reviews;
    @Autowired ListingServiceAvailmentRepository availments;
    @Autowired ListingReviewRequestRepository invitations;
    @Autowired VouchRequestRepository vouchRequests;
    @Autowired VouchRepository vouches;
    @Autowired EntityManager entityManager;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    User buyer, lister;

    @BeforeEach void setup() {
        buyer = user(AccessTypeEnum.BUYER); lister = user(AccessTypeEnum.LISTER);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void emptyBuyerSummaryReturnsZeros() throws Exception {
        mvc.perform(get("/api/v1/buyer/dashboard").with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString()))))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.servicesAvailedCount").value(0)).andExpect(jsonPath("$.submittedReviewCount").value(0))
            .andExpect(jsonPath("$.pendingReviewInvitationCount").value(0)).andExpect(jsonPath("$.pendingVouchRequestsReceived").value(0))
            .andExpect(jsonPath("$.activeVouchesGiven").value(0));
    }
    @Test void buyerMetricsAreScopedAndUseRealStatuses() throws Exception {
        User other = user(AccessTypeEnum.BUYER);
        Listing first = listing(ListingStatus.ACTIVE); Listing second = listing(ListingStatus.INACTIVE);
        Listing deleted = listing(ListingStatus.DELETED);
        availed(first,buyer,-1); availed(second,buyer,-31); availed(first,other,-1); availed(deleted,buyer,-1);
        review(first,buyer,ReviewStatus.APPROVED); review(second,buyer,ReviewStatus.PENDING);
        review(first,buyer,ReviewStatus.REJECTED); review(first,other,ReviewStatus.APPROVED); review(deleted,buyer,ReviewStatus.APPROVED);
        VouchRequestEntity request = new VouchRequestEntity(); request.setRequester(lister); request.setTarget(buyer);
        request.setMessage("Please vouch"); request.setStatus(VouchRequestStatus.PENDING); vouchRequests.saveAndFlush(request);
        mvc.perform(get("/api/v1/buyer/dashboard").param("userId",other.getId().toString())
            .with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString())))).andExpect(status().isOk())
            .andExpect(jsonPath("$.servicesAvailedCount").value(2)).andExpect(jsonPath("$.uniqueProvidersUsed").value(1))
            .andExpect(jsonPath("$.servicesAvailedLast30Days").value(1)).andExpect(jsonPath("$.submittedReviewCount").value(3))
            .andExpect(jsonPath("$.approvedReviewCount").value(1)).andExpect(jsonPath("$.pendingReviewCount").value(1))
            .andExpect(jsonPath("$.rejectedReviewCount").value(1)).andExpect(jsonPath("$.pendingVouchRequestsReceived").value(1));
    }
    @Test void invitationCountExcludesExpiredUndeliveredAndAlreadyReviewed() {
        Listing valid = listing(ListingStatus.ACTIVE); invite(valid,buyer,30,true);
        invite(listing(ListingStatus.ACTIVE),buyer,-1,true);
        invite(listing(ListingStatus.ACTIVE),buyer,30,false);
        invite(listing(ListingStatus.INACTIVE),buyer,30,true);
        Listing reviewed = listing(ListingStatus.ACTIVE); invite(reviewed,buyer,30,true); review(reviewed,buyer,ReviewStatus.PENDING);
        invite(listing(ListingStatus.ACTIVE),user(AccessTypeEnum.BUYER),30,true);
        assertEquals(1,service.buyer(buyer.getId()).pendingReviewInvitationCount());
    }
    @Test void activeVouchesGivenExcludesRevokedAndOtherBuyers() {
        VouchRequestEntity request = new VouchRequestEntity(); request.setRequester(lister); request.setTarget(buyer);
        request.setMessage("Test"); request.setStatus(VouchRequestStatus.ACCEPTED); vouchRequests.saveAndFlush(request);
        Vouch vouch = new Vouch(); vouch.setVoucherUser(buyer); vouch.setVouchedUser(lister); vouch.setRequest(request);
        vouch.setStatus(VouchStatus.ACTIVE); vouches.saveAndFlush(vouch);
        assertEquals(1,service.buyer(buyer.getId()).activeVouchesGiven());
        vouch.setStatus(VouchStatus.REVOKED); vouches.saveAndFlush(vouch);
        assertEquals(0,service.buyer(buyer.getId()).activeVouchesGiven());
    }
    @Test void adminSummaryUsesPlatformScopeAndActualModerationQueue() throws Exception {
        grantAdmin(buyer);
        listing(ListingStatus.ACTIVE); listing(ListingStatus.INACTIVE); listing(ListingStatus.DELETED);
        User old = user(AccessTypeEnum.BUYER);
        entityManager.createQuery("update User u set u.createdAt = :old where u.id = :id")
            .setParameter("old",Instant.now().minus(8,ChronoUnit.DAYS)).setParameter("id",old.getId()).executeUpdate();
        User deleted = user(AccessTypeEnum.BUYER); deleted.setStatus(UserStatus.DELETED); users.saveAndFlush(deleted);
        Listing archived = listing(ListingStatus.DELETED); review(archived,lister,ReviewStatus.PENDING);
        mvc.perform(get("/api/v1/admin/dashboard").with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers").value(3)).andExpect(jsonPath("$.activeUsers").value(3))
            .andExpect(jsonPath("$.newUsersLast7Days").value(2)).andExpect(jsonPath("$.totalListings").value(2))
            .andExpect(jsonPath("$.activeListings").value(1)).andExpect(jsonPath("$.newListingsLast7Days").value(2))
            .andExpect(jsonPath("$.pendingReviewCount").value(1)).andExpect(jsonPath("$.pendingVouchWithdrawalCount").value(0))
            .andExpect(jsonPath("$.pendingScamReportCount").value(0)).andExpect(jsonPath("$.pendingAppealCount").value(0))
            .andExpect(jsonPath("$.unresolvedFeedbackCount").value(0));
    }
    @Test void wrongAccountsAndStaleAdminClaimsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/buyer/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/admin/dashboard")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/buyer/dashboard").with(jwt().jwt(jwt -> jwt.subject(lister.getId().toString())))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/dashboard").with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString())))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/dashboard").with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))).andExpect(status().isForbidden());
        grantAdmin(buyer); buyer.setStatus(UserStatus.SUSPENDED); users.saveAndFlush(buyer);
        mvc.perform(get("/api/v1/admin/dashboard").with(jwt().jwt(jwt -> jwt.subject(buyer.getId().toString()))
            .authorities(new SimpleGrantedAuthority("ROLE_ADMIN")))).andExpect(status().isForbidden());
    }
    private User user(AccessTypeEnum type) {
        User user = new User(); user.setFirstName("Test"); user.setLastName("User"); user.setEmail(UUID.randomUUID()+"@example.com");
        user.setPhoneNumber(UUID.randomUUID().toString().substring(0,18)); user.setPasswordHash("test-only");
        user.setAccessType(type); user.setStatus(UserStatus.ACTIVE); return users.saveAndFlush(user);
    }
    private Listing listing(ListingStatus status) {
        Listing listing = new Listing(); listing.setLister(lister); listing.setListingTitle("Test"); listing.setListingCategory("Catering");
        listing.setDescription("Test"); listing.setServiceType("Cooking"); listing.setSuburb("Suburb"); listing.setState("State");
        listing.setPostcode("1000"); listing.setAvailability("MONDAY"); listing.setPricingType(ListingPricingType.FREE); listing.setStatus(status);
        return listings.saveAndFlush(listing);
    }
    private void availed(Listing listing, User user, int offset) {
        var record = new ListingServiceAvailment(); record.setListing(listing); record.setBuyer(user);
        record.setAvailedAt(Instant.now().plus(offset,ChronoUnit.DAYS)); availments.saveAndFlush(record);
    }
    private void review(Listing listing, User user, ReviewStatus status) {
        var review = new ListingReview(); review.setListing(listing); review.setReviewer(user); review.setRating(4);
        review.setComment("Test"); review.setStatus(status); reviews.saveAndFlush(review);
    }
    private void invite(Listing listing, User user, int offset, boolean sent) {
        var request = new ListingReviewRequest(); request.setListing(listing); request.setBuyer(user); request.setStatus("PENDING");
        request.setRequestedAt(Instant.now()); request.setExpiresAt(Instant.now().plus(offset,ChronoUnit.DAYS));
        if(sent) request.setEmailSentAt(Instant.now()); invitations.saveAndFlush(request);
    }
    private void grantAdmin(User user) {
        Role role = new Role(); role.setCode("ADMIN"); role.setName("Admin"); role.setActive(true); roles.saveAndFlush(role);
        UserRole assignment = new UserRole(); assignment.setUser(user); assignment.setRole(role); userRoles.saveAndFlush(assignment);
    }
}
