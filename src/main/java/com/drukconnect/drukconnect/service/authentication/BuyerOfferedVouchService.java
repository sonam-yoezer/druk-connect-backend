package com.drukconnect.drukconnect.service.authentication;

import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.authentication.*;
import com.drukconnect.drukconnect.dto.authentication.BuyerOfferedVouchResponses.*;
import com.drukconnect.drukconnect.entity.authentication.*;
import com.drukconnect.drukconnect.enums.authentication.*;
import com.drukconnect.drukconnect.repository.authentication.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class BuyerOfferedVouchService {
    private final UserRepository users;
    private final VouchRepository vouches;
    private final VouchRequestRepository requests;
    private final VouchWithdrawalRequestRepository withdrawals;
    private final VouchService decisions;
    private final AuditService audit;
    public BuyerOfferedVouchService(UserRepository users, VouchRepository vouches, VouchRequestRepository requests,
            VouchWithdrawalRequestRepository withdrawals, VouchService decisions, AuditService audit) {
        this.users = users; this.vouches = vouches; this.requests = requests;
        this.withdrawals = withdrawals; this.decisions = decisions; this.audit = audit;
    }
    @Transactional(readOnly = true)
    public List<Candidate> search(UUID buyerId, String query) {
        buyer(buyerId, false);
        String text = query == null ? "" : query.trim();
        if (text.length() < 2 || text.length() > 100) throw new ApiException(HttpStatus.BAD_REQUEST,
                "INVALID_SEARCH_QUERY", "Enter between 2 and 100 characters");
        return users.searchVouchCandidates(buyerId, text, UserStatus.ACTIVE, AccessTypeEnum.LISTER,
                PageRequest.of(0,20)).stream().map(user -> new Candidate(user.getId(), name(user),
                user.getProfileImageUrl(), user.getLocation(), user.getPublicEmail(),
                vouches.countActiveVouchesByUserId(user.getId()),
                vouches.existsByVoucherUserIdAndVouchedUserIdAndStatus(buyerId,user.getId(),VouchStatus.ACTIVE),
                requests.existsByRequesterIdAndTargetIdAndStatus(user.getId(),buyerId,VouchRequestStatus.PENDING))).toList();
    }
    @Transactional
    public Result give(UUID buyerId, UUID listerId, RequestMetadata meta) {
        User buyer = buyer(buyerId, true);
        if (buyerId.equals(listerId)) throw new ApiException(HttpStatus.BAD_REQUEST,"SELF_VOUCH_NOT_ALLOWED","You cannot vouch for yourself");
        User lister = users.findById(listerId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,"LISTER_NOT_FOUND","Lister not found"));
        if (lister.getStatus() != UserStatus.ACTIVE || lister.getAccessType() != AccessTypeEnum.LISTER)
            throw new ApiException(HttpStatus.FORBIDDEN,"ACTIVE_LISTER_REQUIRED","You can only vouch for an active Lister");
        var existing = vouches.findByVoucherUserIdAndVouchedUserId(buyerId,listerId);
        if (existing.isPresent() && existing.get().getStatus() == VouchStatus.ACTIVE) {
            return result(existing.get(),lister,true);
        }
        if (existing.isPresent() && withdrawals.existsByVouchIdAndStatus(existing.get().getId(),VouchWithdrawalStatus.PENDING))
            throw new ApiException(HttpStatus.CONFLICT,"WITHDRAWAL_PENDING","Wait for the pending withdrawal decision before vouching again");
        var request = requests.findFirstByRequesterIdAndTargetIdAndStatusOrderByRequestedAtDesc(
                listerId,buyerId,VouchRequestStatus.PENDING).orElseGet(() -> {
                    var value = new VouchRequestEntity(); value.setRequester(lister); value.setTarget(buyer);
                    value.setMessage("Buyer volunteered to vouch for this lister."); value.setStatus(VouchRequestStatus.PENDING);
                    return requests.saveAndFlush(value);
                });
        decisions.respond(buyerId,request.getId(),new RespondVouchRequest(true),meta);
        audit.log("VOUCH_GIVEN_BY_BUYER",buyerId,null,listerId,null,meta,
                "{\"vouchRequestId\":\""+request.getId()+"\"}");
        return result(vouches.findByVoucherUserIdAndVouchedUserId(buyerId,listerId).orElseThrow(),lister,false);
    }
    private User buyer(UUID id, boolean lock) {
        User user = (lock ? users.findForProfileUpdate(id) : users.findById(id)).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND,"USER_NOT_FOUND","User not found"));
        if (user.getStatus() != UserStatus.ACTIVE || user.getAccessType() != AccessTypeEnum.BUYER)
            throw new ApiException(HttpStatus.FORBIDDEN,"ACTIVE_BUYER_REQUIRED","An active Buyer account is required to give a vouch");
        return user;
    }
    private Result result(Vouch vouch, User lister, boolean duplicate) {
        return new Result(vouch.getId(),lister.getId(),name(lister),vouch.getVouchedAt(),duplicate,
                duplicate ? "You already vouch for this lister." : "Your vouch has been added successfully.");
    }
    private String name(User user) { return (user.getFirstName()+" "+user.getLastName()).trim(); }
}
