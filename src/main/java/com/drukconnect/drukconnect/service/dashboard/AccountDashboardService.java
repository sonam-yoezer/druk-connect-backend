package com.drukconnect.drukconnect.service.dashboard;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.dashboard.*;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.repository.authentication.UserRepository;
import com.drukconnect.drukconnect.repository.authentication.UserRoleRepository;
import com.drukconnect.drukconnect.repository.dashboard.DashboardMetricsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AccountDashboardService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final DashboardMetricsRepository metrics;

    public AccountDashboardService(UserRepository users, UserRoleRepository roles, DashboardMetricsRepository metrics) {
        this.users = users;
        this.roles = roles;
        this.metrics = metrics;
    }

    @Transactional(readOnly = true)
    public BuyerDashboardResponse buyer(UUID userId) {
        User user = activeUser(userId);
        if (user.getAccessType() != AccessTypeEnum.BUYER) throw forbidden("An active Buyer account is required");
        Instant now = Instant.now();
        var services = metrics.buyerServices(userId, now);
        var reviews = metrics.buyerReviews(userId);
        return new BuyerDashboardResponse(services.services(), services.providers(), services.last30Days(),
                reviews.total(), reviews.approved(), reviews.pending(), reviews.rejected(),
                metrics.pendingReviewInvitations(userId, now), metrics.pendingVouchRequests(userId),
                metrics.activeVouchesGiven(userId), now);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse admin(UUID userId) {
        activeUser(userId);
        if (roles.findRoleCodesByUserId(userId).stream().noneMatch("ADMIN"::equalsIgnoreCase)) {
            throw forbidden("An active Admin role is required");
        }
        Instant now = Instant.now();
        var userStats = metrics.users(now);
        var listingStats = metrics.listings(now);
        return new AdminDashboardResponse(userStats.total(), userStats.active(), userStats.newLast7Days(),
                listingStats.total(), listingStats.active(), listingStats.newLast7Days(), metrics.pendingReviews(),
                metrics.pendingWithdrawals(), metrics.pendingReports(), metrics.pendingAppeals(), metrics.unresolvedFeedback(), now);
    }

    private User activeUser(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) throw forbidden("An active account is required");
        return user;
    }
    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "DASHBOARD_ACCESS_DENIED", message);
    }
}
