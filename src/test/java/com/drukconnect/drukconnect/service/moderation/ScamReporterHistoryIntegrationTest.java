package com.drukconnect.drukconnect.service.moderation;

import com.drukconnect.drukconnect.entity.authentication.*;
import com.drukconnect.drukconnect.entity.moderation.ListerScamReport;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.enums.moderation.ScamReportStatus;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.repository.moderation.ListerScamReportRepository;
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
    "spring.datasource.url=jdbc:h2:mem:reporter_history_test;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "app.mail.enabled=false", "app.sms.enabled=false"
})
@Transactional
class ScamReporterHistoryIntegrationTest {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired UserRoleRepository assignments;
    @Autowired ListerScamReportRepository reports;
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    User admin, reporter, lister;
    String path;
    @BeforeEach void setup() {
        admin = user(AccessTypeEnum.BUYER); reporter = user(AccessTypeEnum.BUYER); lister = user(AccessTypeEnum.LISTER);
        Role role = new Role(); role.setCode("ADMIN"); role.setName("Admin"); role.setActive(true); roles.saveAndFlush(role);
        UserRole assignment = new UserRole(); assignment.setRole(role); assignment.setUser(admin); assignments.saveAndFlush(assignment);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        path = "/api/v1/admin/lister-scam/reporters/"+reporter.getId()+"/history";
    }
    @Test void totalsAndHistorySpanAllListingsAndOutcomesForOnlyThisReporter() throws Exception {
        report(reporter,lister,ScamReportStatus.PENDING,1);
        report(reporter,user(AccessTypeEnum.LISTER),ScamReportStatus.APPROVED,2);
        report(reporter,lister,ScamReportStatus.REJECTED,3);
        report(user(AccessTypeEnum.BUYER),lister,ScamReportStatus.PENDING,4);
        mvc.perform(get(path).with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store"))
            .andExpect(jsonPath("$.totalReports").value(3)).andExpect(jsonPath("$.pendingReports").value(1))
            .andExpect(jsonPath("$.approvedReports").value(1)).andExpect(jsonPath("$.rejectedReports").value(1))
            .andExpect(jsonPath("$.history.totalElements").value(3)).andExpect(jsonPath("$.history.reports[0].status").value("REJECTED"))
            .andExpect(jsonPath("$.history.reports[0].moderationReason").value("Reviewed evidence"))
            .andExpect(jsonPath("$.history.reports[0].sourceListingTitle").value("Original listing title"))
            .andExpect(jsonPath("$.history.reports[0].reporterReportCount").value(3))
            .andExpect(jsonPath("$.email").doesNotExist()).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    @Test void statusFilteringAndPaginationDoNotChangeLifetimeTotals() throws Exception {
        report(reporter,lister,ScamReportStatus.PENDING,1);
        report(reporter,lister,ScamReportStatus.PENDING,2);
        report(reporter,lister,ScamReportStatus.APPROVED,3);
        mvc.perform(get(path).param("status","PENDING").param("size","1").param("page","2").with(asAdmin(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalReports").value(3))
            .andExpect(jsonPath("$.approvedReports").value(1)).andExpect(jsonPath("$.history.totalElements").value(2))
            .andExpect(jsonPath("$.history.reports.length()").value(1)).andExpect(jsonPath("$.history.currentPage").value(2))
            .andExpect(jsonPath("$.history.hasPrevious").value(true)).andExpect(jsonPath("$.history.hasNext").value(false));
    }
    @Test void adminReportCardsIncludeReporterNameAndLifetimeReportCount() throws Exception {
        report(reporter,lister,ScamReportStatus.PENDING,1); report(reporter,lister,ScamReportStatus.APPROVED,2);
        mvc.perform(get("/api/v1/admin/lister-scam/reports").with(asAdmin(admin))).andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.reports[0].reporterName").value("History Member"))
            .andExpect(jsonPath("$.reports[0].reporterReportCount").value(2));
        mvc.perform(get("/api/v1/admin/lister-scam/reports").param("all","true").with(asAdmin(admin)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
    }
    @Test void anonymousMembersAndStaleAdminClaimsCannotReadHistory() throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(jwt().jwt(j -> j.subject(reporter.getId().toString())))).andExpect(status().isForbidden());
        mvc.perform(get(path).with(asAdmin(reporter))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/lister-scam/reports").param("all","true").with(asAdmin(reporter))).andExpect(status().isForbidden());
        admin.setStatus(UserStatus.SUSPENDED); users.saveAndFlush(admin);
        mvc.perform(get(path).with(asAdmin(admin))).andExpect(status().isForbidden());
    }
    @Test void missingReporterAndInvalidPaginationHaveClearErrors() throws Exception {
        mvc.perform(get(path).param("page","0").with(asAdmin(admin))).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("size","51").with(asAdmin(admin))).andExpect(status().isBadRequest());
        mvc.perform(get(path).param("status","UNKNOWN").with(asAdmin(admin))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/admin/lister-scam/reporters/"+UUID.randomUUID()+"/history").with(asAdmin(admin))).andExpect(status().isNotFound());
        mvc.perform(get(path).with(asAdmin(admin))).andExpect(status().isOk()).andExpect(jsonPath("$.totalReports").value(0))
            .andExpect(jsonPath("$.pendingReports").value(0)).andExpect(jsonPath("$.history.reports.length()").value(0));
    }
    private JwtRequestPostProcessor asAdmin(User user) {
        return jwt().jwt(j -> j.subject(user.getId().toString())).authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }
    private User user(AccessTypeEnum type) {
        User value = new User(); value.setFirstName("History"); value.setLastName("Member");
        value.setEmail(UUID.randomUUID()+"@example.com"); value.setPhoneNumber(UUID.randomUUID().toString().substring(0,18));
        value.setPasswordHash("test-only"); value.setAccessType(type); value.setStatus(UserStatus.ACTIVE); return users.saveAndFlush(value);
    }
    private ListerScamReport report(User author, User target, ScamReportStatus status, int seconds) {
        ListerScamReport value = new ListerScamReport(); value.setReporter(author); value.setReportedLister(target);
        value.setSourceListingId(UUID.randomUUID()); value.setSourceListingTitle("Original listing title");
        value.setReason("Suspected scam"); value.setDetails("Report evidence"); value.setStatus(status);
        value.setReportedAt(Instant.parse("2026-11-01T00:00:00Z").plusSeconds(seconds));
        if (status != ScamReportStatus.PENDING) {value.setModeratedBy(admin); value.setModeratedAt(Instant.now()); value.setModerationReason("Reviewed evidence");}
        return reports.saveAndFlush(value);
    }
}
