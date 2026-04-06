package com.comp3334_t67.server.services;


import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import com.comp3334_t67.server.Exceptions.*;
import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {


    private final KeyValidator keyValidator;
    private final UserRepository userRepo;
    private final BlockedUserRepository blockedUserRepo;

    // Get user by email
    public UserDto getUserInfoByEmail(String email) {
        User user = requireUserByEmail(email);
        return UserDto.builder()
                .email(user.getEmail())
                .build();

    }

    // Get user by ID
    public UserDto getUserInfoById(String userId) {
        User user = requireUserById(userId);
        return UserDto.builder()
                .email(user.getEmail())
                .build();
    }
    
    // Delete user by ID
    public void deleteUserById(String userId) {
        User user = requireUserById(userId);
        userRepo.delete(user);
    }

    // Update user's public key
    public void uploadPublicKey(String email, String publicKey) {
        // Validate the public key
        keyValidator.validateAndParseRsaPublicKey(publicKey);

        // Get user
        User user = requireUserByEmail(email);

        // update public key and timestamp
        user.setIdentityPublicKey(publicKey);
        user.setKeyUpdatedAt(LocalDateTime.now());
        userRepo.save(user);
    }

    // Get user's public key
    public KeyDto getPublicKeyByEmail(String email) {
        // Get user
        User user = requireUserByEmail(email);
        // return public key and timestamp
        return KeyDto.builder()
                .publicKey(user.getIdentityPublicKey())
                .uploadedAt(user.getKeyUpdatedAt())
                .build();
    }

    // Get other users' public key
    public KeyDto getPublicKeyById(String userId) {
        // Get user
        User user = requireUserById(userId);
        // return public key and timestamp
        return KeyDto.builder()
                .publicKey(user.getIdentityPublicKey())
                .uploadedAt(user.getKeyUpdatedAt())
                .build();
    }

    // Block another user
    public void blockUser(String blockerEmail, String blockedUserId) {
        User blocker = requireUserByEmail(blockerEmail);
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
        }
    }

    // Unblock another user
    public void unblockUser(String blockerEmail, String blockedUserId) {
        User blocker = requireUserByEmail(blockerEmail);
        User blocked = requireUserById(blockedUserId);

        blockedUserRepo.deleteByUserIdAndBlockedUserId(blocker.getId(), blocked.getId());
    }

    // HELPER METHODS ========================

    // Get user by ID, throw exception if not found
    private User requireUserById(String userId) {
        UUID userUuid = UUID.fromString(userId);
        User user = userRepo.findById(userUuid).orElse(null);
        if (user == null) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        return user;
    }

    // Get user by email, throw exception if not found
    private User requireUserByEmail(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User with email " + email + " not found");
        }
        return user;
    }



}
