package com.drukconnect.drukconnect.service.dashboard;

import com.drukconnect.drukconnect.entity.authentication.*;
import com.drukconnect.drukconnect.entity.listing.Listing;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.listing.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.repository.listing.ListingRepository;
import com.drukconnect.drukconnect.util.VouchRequestEmailSender;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:lister_vouches_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.mail.enabled=false", "app.sms.enabled=false"
})
@Transactional
class ListerVouchIntegrationTest {
    @Autowired UserRepository users;
    @Autowired com.drukconnect.drukconnect.service.authentication.BuyerOfferedVouchService offered;
    @Autowired VouchWithdrawalRequestRepository withdrawals;
    @Autowired VouchRepository vouches;
    @Autowired VouchRequestRepository requests;
    @Autowired ListingRepository listings;
    @Autowired WebApplicationContext context;
    @MockitoBean VouchRequestEmailSender email;
    MockMvc mvc;
    User lister, buyer;
    @BeforeEach void setup() {
        lister = user(AccessTypeEnum.LISTER); buyer = user(AccessTypeEnum.BUYER);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    @Test void receivedVouchesAndRequestsBelongOnlyToSignedInLister() throws Exception {
        vouch(lister, buyer, VouchStatus.ACTIVE);
        vouch(lister, user(AccessTypeEnum.BUYER), VouchStatus.REVOKED);
        vouch(user(AccessTypeEnum.LISTER), user(AccessTypeEnum.BUYER), VouchStatus.ACTIVE);
        mvc.perform(get("/api/v1/lister/vouches").param("userId", UUID.randomUUID().toString())
            .with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.items[0].member.userId").value(buyer.getId().toString()))
            .andExpect(jsonPath("$.items[0].member.passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/lister/vouches/requests").with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.items[0].responseTokenHash").doesNotExist());
    }
    @Test void voucherProfileRespectsEachPrivacySwitchAndCurrentRelationship() throws Exception {
        Vouch vouch = vouch(lister, buyer, VouchStatus.ACTIVE);
        String path = "/api/v1/lister/vouches/members/" + buyer.getId();
        buyer.setShowEmail(false); buyer.setShowPhoneNumber(false); users.saveAndFlush(buyer);
        mvc.perform(get(path).with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.phoneNumber").doesNotExist())
            .andExpect(jsonPath("$.name").value(buyer.getFirstName()+" "+buyer.getLastName()))
            .andExpect(jsonPath("$.bio").value("Community member"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.roles").doesNotExist());
        buyer.setShowEmail(true); users.saveAndFlush(buyer);
        mvc.perform(get(path).with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(buyer.getEmail()))
            .andExpect(jsonPath("$.phoneNumber").doesNotExist());
        buyer.setShowEmail(false); buyer.setShowPhoneNumber(true); users.saveAndFlush(buyer);
        mvc.perform(get(path).with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.phoneNumber").value(buyer.getPhoneNumber()));
        User other = user(AccessTypeEnum.LISTER);
        mvc.perform(get(path).with(jwt().jwt(j -> j.subject(other.getId().toString())))).andExpect(status().isNotFound());
        vouch.setStatus(VouchStatus.WITHDRAWN); vouches.saveAndFlush(vouch);
        mvc.perform(get(path).with(jwt().jwt(j -> j.subject(lister.getId().toString())))).andExpect(status().isNotFound());
    }
    @Test void searchUsesRealMembersAndDuplicateStateWithoutLeakingPrivateEmail() throws Exception {
        buyer.setShowEmail(false); buyer.setShowPhoneNumber(false); users.saveAndFlush(buyer);
        vouch(lister, buyer, VouchStatus.ACTIVE);
        mvc.perform(get("/api/v1/lister/vouches/members/search").param("query", buyer.getFirstName())
            .with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].alreadyVouched").value(true))
            .andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get("/api/v1/lister/vouches/members/search").param("query", buyer.getEmail())
            .with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void sendingRequestPersistsItAndRejectsDuplicates() throws Exception {
        String body = "{\"targetUserId\":\""+buyer.getId()+"\",\"message\":\"Please vouch for me\"}";
        mvc.perform(post("/api/v1/lister/vouches/requests").contentType("application/json").content(body)
            .with(jwt().jwt(j -> j.subject(lister.getId().toString())))).andExpect(status().isCreated());
        assertTrue(requests.existsByRequesterIdAndTargetIdAndStatus(lister.getId(),buyer.getId(),VouchRequestStatus.PENDING));
        mvc.perform(post("/api/v1/lister/vouches/requests").contentType("application/json").content(body)
            .with(jwt().jwt(j -> j.subject(lister.getId().toString())))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/lister/vouches/requests").with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].status").value("PENDING"));
        mvc.perform(get("/api/v1/lister/vouches/members/search").param("query", buyer.getFirstName())
            .with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].requestPending").value(true));
    }
    @Test void listingViewsUseRealListerScopedTotals() throws Exception {
        listing(lister, ListingStatus.ACTIVE, 17); listing(lister, ListingStatus.INACTIVE, 5);
        listing(lister, ListingStatus.DELETED, 200); listing(user(AccessTypeEnum.LISTER), ListingStatus.ACTIVE, 100);
        mvc.perform(get("/api/v1/lister/dashboard").with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalListingViews").value(22));
    }
    @Test void authenticationRoleAndPaginationAreEnforced() throws Exception {
        mvc.perform(get("/api/v1/lister/vouches")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/lister/vouches").with(jwt().jwt(j -> j.subject(buyer.getId().toString())))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/lister/vouches").param("page", "-1").with(jwt().jwt(j -> j.subject(lister.getId().toString())))).andExpect(status().isBadRequest());
        for (int index=0;index<11;index++) vouch(lister,user(AccessTypeEnum.BUYER),VouchStatus.ACTIVE);
        mvc.perform(get("/api/v1/lister/vouches").with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(11))
            .andExpect(jsonPath("$.items.length()").value(10)).andExpect(jsonPath("$.hasNext").value(true));
        mvc.perform(get("/api/v1/lister/vouches").param("page","1").with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
    }
    @Test void buyerCanVouchWithoutARequestAndSeeItOnBothDashboards() throws Exception {
        mvc.perform(post("/api/v1/buyer/vouches/listers/"+lister.getId())
            .param("buyerId",user(AccessTypeEnum.BUYER).getId().toString())
            .with(jwt().jwt(j -> j.subject(buyer.getId().toString())))).andExpect(status().isOk())
            .andExpect(jsonPath("$.alreadyVouched").value(false));
        var value = vouches.findByVoucherUserIdAndVouchedUserId(buyer.getId(),lister.getId()).orElseThrow();
        assertEquals(VouchStatus.ACTIVE,value.getStatus());
        assertEquals(VouchRequestStatus.ACCEPTED,value.getRequest().getStatus());
        assertEquals(1,requests.countByRequesterIdAndStatus(lister.getId(),VouchRequestStatus.ACCEPTED));
        mvc.perform(get("/api/v1/lister/vouches").with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/v1/vouch-requests/me/given-vouches").with(jwt().jwt(j -> j.subject(buyer.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalVouchedUsers").value(1));
    }
    @Test void voluntaryVouchAcceptsExistingPendingRequestInsteadOfCreatingAnother() {
        VouchRequestEntity request = new VouchRequestEntity(); request.setRequester(lister); request.setTarget(buyer);
        request.setMessage("Please vouch"); request.setStatus(VouchRequestStatus.PENDING); requests.saveAndFlush(request);
        var result = offered.give(buyer.getId(),lister.getId(),null);
        var value = vouches.findById(result.vouchId()).orElseThrow();
        assertEquals(request.getId(),value.getRequest().getId());
        assertEquals(1,requests.findByRequesterId(lister.getId(),org.springframework.data.domain.PageRequest.of(0,10)).getTotalElements());
        assertFalse(requests.existsByRequesterIdAndTargetIdAndStatus(lister.getId(),buyer.getId(),VouchRequestStatus.PENDING));
    }
    @Test void listerWithTwoVouchesCanRequestAndReceiveAThird() throws Exception {
        vouch(lister,user(AccessTypeEnum.BUYER),VouchStatus.ACTIVE);
        vouch(lister,user(AccessTypeEnum.BUYER),VouchStatus.ACTIVE);
        mvc.perform(post("/api/v1/lister/vouches/requests").contentType("application/json")
            .content("{\"targetUserId\":\""+buyer.getId()+"\",\"message\":\"Please vouch for me\"}")
            .with(jwt().jwt(j -> j.subject(lister.getId().toString())))).andExpect(status().isCreated());
        offered.give(buyer.getId(),lister.getId(),null);
        assertEquals(3,vouches.countActiveVouchesByUserId(lister.getId()));
        mvc.perform(get("/api/v1/lister/vouches").with(jwt().jwt(j -> j.subject(lister.getId().toString()))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }
    @Test void repeatingVoluntaryVouchIsIdempotent() {
        var first = offered.give(buyer.getId(),lister.getId(),null);
        var second = offered.give(buyer.getId(),lister.getId(),null);
        assertEquals(first.vouchId(),second.vouchId());
        assertTrue(second.alreadyVouched());
        assertEquals(1,vouches.countActiveVouchesByUserId(lister.getId()));
        assertEquals(1,requests.countByRequesterIdAndStatus(lister.getId(),VouchRequestStatus.ACCEPTED));
    }
    @Test void buyerCanVouchAgainAfterWithdrawalUsingSamePairRow() {
        var old = vouch(lister,buyer,VouchStatus.WITHDRAWN); old.setRevokedAt(Instant.now()); vouches.saveAndFlush(old);
        var withdrawal = new VouchWithdrawalRequest(); withdrawal.setVouch(old); withdrawal.setRequestedBy(buyer);
        withdrawal.setReason("Previous withdrawal"); withdrawal.setStatus(VouchWithdrawalStatus.APPROVED); withdrawals.saveAndFlush(withdrawal);
        var result = offered.give(buyer.getId(),lister.getId(),null);
        assertEquals(old.getId(),result.vouchId());
        assertEquals(VouchStatus.ACTIVE,old.getStatus()); assertNull(old.getRevokedAt());
        assertEquals(VouchWithdrawalStatus.APPROVED,withdrawals.findById(withdrawal.getId()).orElseThrow().getStatus());
        assertEquals(1,vouches.countActiveVouchesByUserId(lister.getId()));
    }
    @Test void pendingWithdrawalIsPreservedAndCannotBeBypassed() {
        var old = vouch(lister,buyer,VouchStatus.WITHDRAWN);
        var withdrawal = new VouchWithdrawalRequest(); withdrawal.setVouch(old); withdrawal.setRequestedBy(buyer);
        withdrawal.setReason("Awaiting admin"); withdrawal.setStatus(VouchWithdrawalStatus.PENDING); withdrawals.saveAndFlush(withdrawal);
        var failure = assertThrows(com.drukconnect.drukconnect.common.ApiException.class,
            () -> offered.give(buyer.getId(),lister.getId(),null));
        assertEquals("WITHDRAWAL_PENDING",failure.getCode());
        assertEquals(VouchStatus.WITHDRAWN,old.getStatus());
        assertEquals(VouchWithdrawalStatus.PENDING,withdrawal.getStatus());
    }
    @Test void buyerSearchRespectsEmailPrivacyAndOnlyActiveListersCanReceiveVouches() throws Exception {
        lister.setShowEmail(false); users.saveAndFlush(lister);
        mvc.perform(get("/api/v1/buyer/vouches/listers/search").param("query",lister.getFirstName())
            .with(jwt().jwt(j -> j.subject(buyer.getId().toString())))).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].userId").value(lister.getId().toString())).andExpect(jsonPath("$[0].email").doesNotExist());
        mvc.perform(get("/api/v1/buyer/vouches/listers/search").param("query",lister.getEmail())
            .with(jwt().jwt(j -> j.subject(buyer.getId().toString())))).andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(0));
        lister.setStatus(UserStatus.SUSPENDED); users.saveAndFlush(lister);
        mvc.perform(post("/api/v1/buyer/vouches/listers/"+lister.getId())
            .with(jwt().jwt(j -> j.subject(buyer.getId().toString())))).andExpect(status().isForbidden());
    }
    @Test void buyerOfferedVouchesRequireAuthenticationAndBuyerRole() throws Exception {
        mvc.perform(post("/api/v1/buyer/vouches/listers/"+lister.getId())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/buyer/vouches/listers/"+lister.getId())
            .with(jwt().jwt(j -> j.subject(lister.getId().toString())))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/buyer/vouches/listers/"+buyer.getId())
            .with(jwt().jwt(j -> j.subject(buyer.getId().toString())))).andExpect(status().isBadRequest());
    }
    private User user(AccessTypeEnum type) {
        User user = new User(); user.setFirstName("Member"+UUID.randomUUID()); user.setLastName("Test");
        user.setEmail(UUID.randomUUID()+"@example.com"); user.setPhoneNumber(UUID.randomUUID().toString().substring(0,18));
        user.setPasswordHash("test-only"); user.setAccessType(type); user.setStatus(UserStatus.ACTIVE);
        user.setBio("Community member"); user.setLocation("Melbourne"); return users.saveAndFlush(user);
    }
    private Vouch vouch(User owner,User member,VouchStatus status) {
        VouchRequestEntity request = new VouchRequestEntity(); request.setRequester(owner); request.setTarget(member);
        request.setMessage("Please vouch"); request.setStatus(VouchRequestStatus.ACCEPTED); requests.saveAndFlush(request);
        Vouch value = new Vouch(); value.setVoucherUser(member); value.setVouchedUser(owner); value.setRequest(request);
        value.setStatus(status); value.setVouchedAt(Instant.now()); return vouches.saveAndFlush(value);
    }
    private void listing(User owner, ListingStatus status, long views) {
        Listing value = new Listing(); value.setLister(owner); value.setListingTitle("Test"); value.setListingCategory("Catering");
        value.setDescription("Test"); value.setServiceType("Cooking"); value.setSuburb("Suburb"); value.setState("State");
        value.setPostcode("1000"); value.setAvailability("MONDAY"); value.setPricingType(ListingPricingType.FREE);
        value.setStatus(status); value.setViews(views); listings.saveAndFlush(value);
    }
}
