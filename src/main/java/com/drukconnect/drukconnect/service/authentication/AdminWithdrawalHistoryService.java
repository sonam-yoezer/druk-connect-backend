package com.drukconnect.drukconnect.service.authentication;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.authentication.AdminWithdrawalHistoryResponses.*;
import com.drukconnect.drukconnect.entity.authentication.VouchWithdrawalRequest;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminWithdrawalHistoryService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final VouchWithdrawalRequestRepository withdrawals;
    public AdminWithdrawalHistoryService(UserRepository users, UserRoleRepository roles,
            VouchWithdrawalRequestRepository withdrawals) {
        this.users = users; this.roles = roles; this.withdrawals = withdrawals;
    }
    public Results list(UUID adminId, String status, int page, int size) {
        requireAdmin(adminId);
        var filter = status(status);
        var pageable = pageable(page, size);
        var result = filter == null ? withdrawals.findAll(pageable) : withdrawals.findAllByStatus(filter, pageable);
        Map<UUID, VouchWithdrawalRequestRepository.ListerStats> stats = new HashMap<>();
        return Results.from(result.map(request -> item(request, stats.computeIfAbsent(
                request.getVouch().getVouchedUser().getId(), withdrawals::getListerStats))));
    }
    public History history(UUID adminId, UUID listerId, String status, int page, int size) {
        requireAdmin(adminId);
        var filter = status(status);
        var pageable = pageable(page, size);
        var lister = users.findById(listerId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "LISTER_NOT_FOUND", "Lister not found"));
        if (lister.getAccessType() != AccessTypeEnum.LISTER) throw new ApiException(
                HttpStatus.NOT_FOUND, "LISTER_NOT_FOUND", "Lister not found");
        var stats = withdrawals.getListerStats(listerId);
        var result = filter == null ? withdrawals.findAllByVouchVouchedUserId(listerId, pageable)
                : withdrawals.findAllByVouchVouchedUserIdAndStatus(listerId, filter, pageable);
        return new History(listerId, (lister.getFirstName() + " " + lister.getLastName()).trim(),
                stats.getTotalRequests(), stats.getUniqueRequesters(), stats.getPendingRequests(),
                stats.getApprovedRequests(), stats.getRejectedRequests(), stats.getFirstRequestedAt(),
                stats.getLastRequestedAt(), Results.from(result.map(request -> item(request, stats))));
    }
    private Item item(VouchWithdrawalRequest request, VouchWithdrawalRequestRepository.ListerStats stats) {
        var vouch = request.getVouch();
        var buyer = request.getRequestedBy();
        var lister = vouch.getVouchedUser();
        return new Item(request.getId(), vouch.getId(), buyer.getId(),
                (buyer.getFirstName() + " " + buyer.getLastName()).trim(), buyer.getPublicEmail(), buyer.getPublicPhoneNumber(),
                lister.getId(), (lister.getFirstName() + " " + lister.getLastName()).trim(),
                lister.getPublicEmail(), lister.getPublicPhoneNumber(), request.getReason(), request.getStatus().name(),
                vouch.getVouchedAt(), request.getRequestedAt(), request.getModeratedAt(), request.getAdminReason(),
                stats.getTotalRequests(), stats.getUniqueRequesters());
    }
    private void requireAdmin(UUID adminId) {
        var user = users.findById(adminId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE
                || roles.findRoleCodesByUserId(adminId).stream().noneMatch("ADMIN"::equalsIgnoreCase)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ADMIN_REQUIRED", "An active Admin account is required to view withdrawal history");
        }
    }
    private VouchWithdrawalStatus status(String value) {
        if (value == null || value.equals("ALL")) return null;
        try { return VouchWithdrawalStatus.valueOf(value); }
        catch (IllegalArgumentException exception) { throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_WITHDRAWAL_STATUS", "Status must be ALL, PENDING, APPROVED, or REJECTED"); }
    }
    private PageRequest pageable(int page, int size) {
        if (page < 1 || size < 1 || size > 50) throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_PAGINATION", "Page must be at least 1 and size must be between 1 and 50");
        return PageRequest.of(page - 1, size, Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id")));
    }
}
