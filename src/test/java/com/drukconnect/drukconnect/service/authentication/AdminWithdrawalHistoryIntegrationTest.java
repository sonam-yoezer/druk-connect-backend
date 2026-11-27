package com.drukconnect.drukconnect.service.authentication;

import com.drukconnect.drukconnect.entity.authentication.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:withdrawal_history_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.mail.enabled=false", "app.sms.enabled=false"
})
@Transactional
class AdminWithdrawalHistoryIntegrationTest {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired UserRoleRepository assignments;
    @Autowired VouchRepository vouches;
    @Autowired VouchRequestRepository requests;
    @Autowired VouchWithdrawalRequestRepository withdrawals;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    User admin, buyer, lister;
    String path;
    @BeforeEach void setup() {
        admin = user(AccessTypeEnum.BUYER); buyer = user(AccessTypeEnum.BUYER); lister = user(AccessTypeEnum.LISTER);
        Role role = new Role(); role.setCode("ADMIN"); role.setName("Admin"); roles.saveAndFlush(role);
        UserRole assignment = new UserRole(); assignment.setUser(admin); assignment.setRole(role); assignments.saveAndFlush(assignment);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        path = "/api/v1/admin/vouch-withdrawals/listers/"+lister.getId()+"/history";
    }
    @Test void lifetimeTotalsCountRequestsAndDistinctPeopleSeparatelyAndExcludeOtherListers() throws Exception {
        Vouch first = vouch(lister,buyer);
        withdrawal(first,VouchWithdrawalStatus.REJECTED); withdrawal(first,VouchWithdrawalStatus.PENDING);
        withdrawal(vouch(lister,user(AccessTypeEnum.BUYER)),VouchWithdrawalStatus.APPROVED);
        withdrawal(vouch(user(AccessTypeEnum.LISTER),buyer),VouchWithdrawalStatus.PENDING);
        mvc.perform(get(path).with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.totalRequests").value(3)).andExpect(jsonPath("$.uniqueRequesters").value(2))
            .andExpect(jsonPath("$.pendingRequests").value(1)).andExpect(jsonPath("$.approvedRequests").value(1))
            .andExpect(jsonPath("$.rejectedRequests").value(1)).andExpect(jsonPath("$.history.totalElements").value(3))
            .andExpect(jsonPath("$.history.requests[0].listerUserId").value(lister.getId().toString()))
            .andExpect(jsonPath("$.history.requests[0].listerRequestCount").value(3))
            .andExpect(jsonPath("$.history.requests[0].listerUniqueRequesterCount").value(2));
    }
    @Test void filtersAndPaginationKeepLifetimeSummaryAndReviewedReasons() throws Exception {
        Vouch value = vouch(lister,buyer);
        withdrawal(value,VouchWithdrawalStatus.PENDING); withdrawal(value,VouchWithdrawalStatus.PENDING);
        withdrawal(value,VouchWithdrawalStatus.REJECTED);
        mvc.perform(get(path).param("status","PENDING").param("size","1").param("page","2").with(asAdmin(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalRequests").value(3))
            .andExpect(jsonPath("$.uniqueRequesters").value(1)).andExpect(jsonPath("$.history.totalElements").value(2))
            .andExpect(jsonPath("$.history.requests.length()").value(1)).andExpect(jsonPath("$.history.hasPrevious").value(true))
            .andExpect(jsonPath("$.history.hasNext").value(false));
        mvc.perform(get(path).param("status","REJECTED").with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.history.requests[0].adminReason").value("Reviewed evidence"))
            .andExpect(jsonPath("$.history.requests[0].moderatedAt").isNotEmpty());
    }
    @Test void adminListDefaultsToPendingAndCanBrowseAllOutcomesWithListerCounts() throws Exception {
        Vouch value = vouch(lister,buyer);
        withdrawal(value,VouchWithdrawalStatus.PENDING); withdrawal(value,VouchWithdrawalStatus.APPROVED);
        mvc.perform(get("/api/v1/admin/vouch-withdrawals").with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.requests[0].listerRequestCount").value(2))
            .andExpect(jsonPath("$.requests[0].listerUniqueRequesterCount").value(1));
        mvc.perform(get("/api/v1/admin/vouch-withdrawals").param("status","ALL").with(asAdmin(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }
    @Test void historyAndQueueRespectEmailAndPhoneVisibility() throws Exception {
        buyer.setShowEmail(false); buyer.setShowPhoneNumber(false); users.saveAndFlush(buyer);
        lister.setShowEmail(false); lister.setShowPhoneNumber(false); users.saveAndFlush(lister);
        withdrawal(vouch(lister,buyer),VouchWithdrawalStatus.PENDING);
        mvc.perform(get(path).with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.history.requests[0].buyerEmail").doesNotExist())
            .andExpect(jsonPath("$.history.requests[0].buyerPhoneNumber").doesNotExist())
            .andExpect(jsonPath("$.history.requests[0].listerEmail").doesNotExist())
            .andExpect(jsonPath("$.history.requests[0].listerPhoneNumber").doesNotExist())
            .andExpect(jsonPath("$.history.requests[0].passwordHash").doesNotExist());
        mvc.perform(get("/api/v1/admin/vouch-withdrawals").with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.requests[0].buyerEmail").doesNotExist());
    }
    @Test void onlyCurrentActiveAdminsCanReadHistoryOrList() throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(jwt().jwt(j -> j.subject(buyer.getId().toString())))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(asAdmin(buyer))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/vouch-withdrawals").with(asAdmin(buyer))).andExpect(status().isForbidden());
        admin.setStatus(UserStatus.SUSPENDED); users.saveAndFlush(admin);
        mvc.perform(get(path).with(asAdmin(admin))).andExpect(status().isForbidden());
    }
    @Test void emptyHistoryMissingListersAndBadFiltersHaveDefinedResponses() throws Exception {
        mvc.perform(get(path).with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalRequests").value(0)).andExpect(jsonPath("$.uniqueRequesters").value(0))
            .andExpect(jsonPath("$.history.requests.length()").value(0));
        mvc.perform(get(path).param("page","0").with(asAdmin(admin))).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("size","51").with(asAdmin(admin))).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("status","UNKNOWN").with(asAdmin(admin))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/vouch-withdrawals/listers/"+UUID.randomUUID()+"/history").with(asAdmin(admin))).andExpect(status().isNotFound());
    }
    private JwtRequestPostProcessor asAdmin(User user) {
        return jwt().jwt(j -> j.subject(user.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    private User user(AccessTypeEnum type) {
        User value = new User(); value.setFirstName("Withdrawal"); value.setLastName("Member");
        value.setEmail(UUID.randomUUID()+"@example.com"); value.setPhoneNumber(UUID.randomUUID().toString().substring(0,18));
        value.setPasswordHash("test-only"); value.setAccessType(type); value.setStatus(UserStatus.ACTIVE); return users.saveAndFlush(value);
    }
    private Vouch vouch(User owner,User member) {
        VouchRequestEntity request = new VouchRequestEntity(); request.setRequester(owner); request.setTarget(member);
        request.setMessage("Please vouch"); request.setStatus(VouchRequestStatus.ACCEPTED); requests.saveAndFlush(request);
        Vouch value = new Vouch(); value.setVoucherUser(member); value.setVouchedUser(owner); value.setRequest(request);
        value.setStatus(VouchStatus.ACTIVE); return vouches.saveAndFlush(value);
    }
    private void withdrawal(Vouch vouch,VouchWithdrawalStatus status) {
        VouchWithdrawalRequest value = new VouchWithdrawalRequest(); value.setVouch(vouch); value.setRequestedBy(vouch.getVoucherUser());
        value.setReason("Withdrawal evidence"); value.setStatus(status);
        if(status != VouchWithdrawalStatus.PENDING) { value.setModeratedBy(admin); value.setModeratedAt(Instant.now()); value.setAdminReason("Reviewed evidence"); }
        withdrawals.saveAndFlush(value);
    }
}
