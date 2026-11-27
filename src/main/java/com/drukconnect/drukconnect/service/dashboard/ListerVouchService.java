package com.drukconnect.drukconnect.service.dashboard;

import com.drukconnect.drukconnect.common.ApiException;
import com.drukconnect.drukconnect.dto.dashboard.ListerVouchResponses.*;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListerVouchService {
    private final UserRepository users;
    private final VouchRepository vouches;
    private final VouchRequestRepository requests;
    public ListerVouchService(UserRepository users, VouchRepository vouches, VouchRequestRepository requests) {
        this.users = users; this.vouches = vouches; this.requests = requests;
    }
    public Results<Received> received(UUID listerId, int page) {
        requireLister(listerId);
        return Results.from(vouches.findByVouchedUserIdAndStatus(listerId, VouchStatus.ACTIVE,
                pageable(page, "vouchedAt")).map(vouch -> new Received(vouch.getId(),
                        member(vouch.getVoucherUser()), vouch.getVouchedAt())));
    }
    public Results<Sent> sent(UUID listerId, int page) {
        requireLister(listerId);
        return Results.from(requests.findByRequesterId(listerId, pageable(page, "requestedAt"))
                .map(request -> new Sent(request.getId(), name(request.getTarget()),
                        request.getTarget().getProfileImageUrl(), request.getStatus().name(),
                        request.getRequestedAt(), request.getRespondedAt())));
    }
    public Member profile(UUID listerId, UUID memberId) {
        requireLister(listerId);
        // A member ID alone must not grant access to their account profile.
        if (!vouches.existsByVoucherUserIdAndVouchedUserIdAndStatus(memberId, listerId, VouchStatus.ACTIVE)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "VOUCHER_NOT_FOUND", "This member has no active vouch for your account");
        }
        return member(users.findById(memberId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Member not found")));
    }
    public List<Candidate> search(UUID listerId, String query) {
        requireLister(listerId);
        String text = query == null ? "" : query.trim();
        if (text.length() < 2 || text.length() > 100) throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_SEARCH_QUERY", "Enter between 2 and 100 characters to search members");
        return users.searchVouchCandidates(listerId, text, UserStatus.ACTIVE, AccessTypeEnum.BUYER,
                PageRequest.of(0, 20)).stream().map(user -> new Candidate(user.getId(), name(user),
                user.getProfileImageUrl(), user.getLocation(), user.getPublicEmail(),
                vouches.existsByVoucherUserIdAndVouchedUserIdAndStatus(user.getId(), listerId, VouchStatus.ACTIVE),
                requests.existsByRequesterIdAndTargetIdAndStatus(listerId, user.getId(), VouchRequestStatus.PENDING))).toList();
    }
    private User requireLister(UUID id) {
        User user = users.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getAccessType() != AccessTypeEnum.LISTER || user.getStatus() != UserStatus.ACTIVE)
            throw new ApiException(HttpStatus.FORBIDDEN, "ACTIVE_LISTER_REQUIRED", "An active Lister account is required");
        return user;
    }
    private PageRequest pageable(int page, String field) {
        if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "Page must be zero or greater");
        return PageRequest.of(page, 10, Sort.by(Sort.Order.desc(field), Sort.Order.desc("id")));
    }
    private Member member(User user) {
        return new Member(user.getId(), name(user), user.getProfileImageUrl(), user.getLocation(),
                user.getBio(), user.getPublicEmail(), user.getPublicPhoneNumber());
    }
    private String name(User user) { return (user.getFirstName() + " " + user.getLastName()).trim(); }
}
