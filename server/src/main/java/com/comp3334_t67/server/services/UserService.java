package com.comp3334_t67.server.services;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.comp3334_t67.server.Exceptions.*;
import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);


    private final KeyValidator keyValidator;
    private final UserRepository userRepo;
    private final BlockedUserRepository blockedUserRepo;

    // Get user by email
    public UserDto getUserInfoByEmail(String email) {
        log.info("Fetching user info by email={}", email);
        User user = requireUserByEmail(email);
        log.info("Fetched user info by email={}", email);
        return UserDto.builder()
                .email(user.getEmail())
                .build();

    }

    // Get user by ID
    public UserDto getUserInfoById(String userId) {
        log.info("Fetching user info by userId={}", userId);
        User user = requireUserById(userId);
        log.info("Fetched user info by userId={}", userId);
        return UserDto.builder()
                .email(user.getEmail())
                .build();
    }
    
    // Delete user by ID
    public void deleteUserById(String userId) {
        log.info("Deleting user by userId={}", userId);
        User user = requireUserById(userId);
        userRepo.delete(user);
        log.info("Deleted user by userId={}", userId);
    }

    // Update user's public key
    public void uploadPublicKey(String email, String publicKey) {
        log.info("Updating public key for user={}", email);
        // Validate the public key
        keyValidator.validateAndParseRsaPublicKey(publicKey);

        // Get user
        User user = requireUserByEmail(email);

        // update public key and timestamp
        user.setIdentityPublicKey(publicKey);
        user.setKeyUpdatedAt(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        userRepo.save(user);
        log.info("Updated public key for user={}", email);
    }

    // Get user's public key
    public KeyDto getPublicKeyByEmail(String email) {
        log.info("Fetching public key by email={}", email);
        // Get user
        User user = requireUserByEmail(email);
        // return public key and timestamp
        log.info("Fetched public key by email={}", email);
        return KeyDto.builder()
                .publicKey(user.getIdentityPublicKey())
                .uploadedAt(user.getKeyUpdatedAt())
                .build();
    }

    // Get other users' public key
    public KeyDto getPublicKeyById(String userId) {
        log.info("Fetching public key by userId={}", userId);
        // Get user
        User user = requireUserById(userId);
        // return public key and timestamp
        log.info("Fetched public key by userId={}", userId);
        return KeyDto.builder()
                .publicKey(user.getIdentityPublicKey())
                .uploadedAt(user.getKeyUpdatedAt())
                .build();
    }

    // Block another user
    public void blockUser(String blockerEmail, String blockedUserId) {
        log.info("Blocking user request received from={} targetId={}", blockerEmail, blockedUserId);
        User blocker = requireUserByEmail(blockerEmail);

        if (blocker.getId().toString().equals(blockedUserId)) {
            throw new SelfBlockNotAllowedException("User cannot block self");
        }

        User blocked = requireUserById(blockedUserId);

        if (blocker.getId().equals(blocked.getId())) {
            throw new SelfBlockNotAllowedException("User cannot block self");
        }

        if (!blockedUserRepo.existsByUserIdAndBlockedUserId(blocker.getId(), blocked.getId())) {
            blockedUserRepo.save(
                BlockedUser.builder()
                    .userId(blocker.getId())
                    .blockedUserId(blocked.getId())
                    .blockedAt(LocalDateTime.now())
                    .build()
            );
            log.info("Blocked user successfully from={} targetId={}", blockerEmail, blockedUserId);
        }
    }

    // Unblock another user
    @Transactional
    public void unblockUser(String blockerEmail, String blockedUserId) {
        log.info("Unblocking user request received from={} targetId={}", blockerEmail, blockedUserId);
        User blocker = requireUserByEmail(blockerEmail);
        User blocked = requireUserById(blockedUserId);

        blockedUserRepo.deleteByUserIdAndBlockedUserId(blocker.getId(), blocked.getId());
        log.info("Unblocked user successfully from={} targetId={}", blockerEmail, blockedUserId);
    }

    // HELPER METHODS ========================

    // Get user by ID, throw exception if not found
    private User requireUserById(String userId) {
        log.info("Resolving user by id={}", userId);
        UUID userUuid = UUID.fromString(userId);
        User user = userRepo.findById(userUuid).orElse(null);
        if (user == null) {
            log.warn("User lookup failed for id={}", userId);
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        log.info("Resolved user by id={}", userId);
        return user;
    }

    // Get user by email, throw exception if not found
    private User requireUserByEmail(String email) {
        log.info("Resolving user by email={}", email);
        User user = userRepo.findByEmail(email);
        if (user == null) {
            log.warn("User lookup failed for email={}", email);
            throw new UserNotFoundException("User with email " + email + " not found");
        }
        log.info("Resolved user by email={}", email);
        return user;
    }



}
