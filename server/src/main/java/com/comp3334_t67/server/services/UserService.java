package com.comp3334_t67.server.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import com.comp3334_t67.server.Exceptions.SelfBlockNotAllowedException;
import com.comp3334_t67.server.Exceptions.UserNotFoundException;
import com.comp3334_t67.server.models.BlockedUser;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.dtos.UserDto;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    
    private final UserRepository userRepo;
    private final BlockedUserRepository blockedUserRepo;

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
    public void uploadPublicKey(String userId, String publicKey) {
        User user = requireUserById(userId);
        
        user.setIdentityPublicKey(publicKey);
        userRepo.save(user);
    }

    // Get user's public key
    public String getPublicKey(String userId) {
        User user = requireUserById(userId);
        
        return user.getIdentityPublicKey();
    }

    // Block another user
    public void blockUser(String blockerEmail, String blockedEmail) {
        User blocker = requireUserByEmail(blockerEmail);
        User blocked = requireUserByEmail(blockedEmail);

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
    public void unblockUser(String blockerEmail, String blockedEmail) {
        User blocker = requireUserByEmail(blockerEmail);
        User blocked = requireUserByEmail(blockedEmail);

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
