package com.drukconnect.drukconnect.service.profile;

import com.drukconnect.drukconnect.common.*;
import com.drukconnect.drukconnect.dto.profile.*;
import com.drukconnect.drukconnect.entity.authentication.User;
import com.drukconnect.drukconnect.enums.authentication.UserStatus;
import com.drukconnect.drukconnect.repository.authentication.*;
import com.drukconnect.drukconnect.service.authentication.AuditService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

@Service
public class UserProfileService {
    private final UserRepository users;
    private final UserRoleRepository roles;
    private final ProfileImageStorageService images;
    private final AuditService audit;

    public UserProfileService(UserRepository users, UserRoleRepository roles, ProfileImageStorageService images, AuditService audit) {
        this.users = users; this.roles = roles; this.images = images; this.audit = audit;
    }
    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) { return response(requireActive(userId, false)); }

    @Transactional
    public ProfileResponse update(UUID userId, UpdateProfileRequest request, RequestMetadata meta) {
        User user = requireActive(userId, true);
        user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setBio(request.bio() == null ? null : request.bio().trim());
        user.setLocation(request.location() == null ? null : request.location().trim());
        users.saveAndFlush(user);
        audit.log("PROFILE_UPDATED", userId, null, null, null, meta, null);
        return response(user);
    }
    @Transactional
    public ProfileResponse upload(UUID userId, MultipartFile image, RequestMetadata meta) {
        User user = requireActive(userId, true);
        String previous = user.getProfileImagePath();
        String uploaded = images.store(userId, image);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) images.delete(uploaded);
            }
            @Override public void afterCommit() { images.delete(previous); }
        });
        user.setProfileImagePath(uploaded); users.saveAndFlush(user);
        audit.log("PROFILE_IMAGE_UPDATED", userId, null, null, null, meta, null);
        return response(user);
    }
    @Transactional
    public ProfileResponse removeImage(UUID userId, RequestMetadata meta) {
        User user = requireActive(userId, true);
        String previous = user.getProfileImagePath();
        user.setProfileImagePath(null); users.saveAndFlush(user);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { images.delete(previous); }
        });
        audit.log("PROFILE_IMAGE_REMOVED", userId, null, null, null, meta, null);
        return response(user);
    }
    @Transactional(readOnly = true)
    public Resource image(UUID userId) {
        User user = users.findById(userId).filter(value -> value.getStatus() == UserStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROFILE_IMAGE_NOT_FOUND", "Profile image not found"));
        if (user.getProfileImagePath() == null) throw new ApiException(HttpStatus.NOT_FOUND, "PROFILE_IMAGE_NOT_FOUND", "Profile image not found");
        return images.load(user.getProfileImagePath());
    }
    private User requireActive(UUID id, boolean lock) {
        User user = (lock ? users.findForProfileUpdate(id) : users.findById(id))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found"));
        if (user.getStatus() != UserStatus.ACTIVE) throw new ApiException(HttpStatus.FORBIDDEN, "ACTIVE_ACCOUNT_REQUIRED", "An active account is required");
        return user;
    }
    private ProfileResponse response(User user) {
        return new ProfileResponse(user.getId(), user.getFirstName(), user.getLastName(), user.getEmail(), user.getPhoneNumber(),
                user.getAccessType(), roles.findRoleCodesByUserId(user.getId()), user.getBio(), user.getLocation(),
                user.getProfileImageUrl(), user.getEmailVerifiedAt(), user.getPhoneVerifiedAt());
    }
}
