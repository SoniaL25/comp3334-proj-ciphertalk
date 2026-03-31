package com.comp3334_t67.server.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import com.comp3334_t67.server.models.BlockedUser;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    
    private final UserRepository userRepo;
    private final BlockedUserRepository blockedUserRepo;

    public void uploadPublicKey(String userId, String publicKey) {
        User user = requireUserById(userId);
        
        user.setIdentity_public_key(publicKey);
        userRepo.save(user);
    }

    public String getPublicKey(String userId) {
        User user = requireUserById(userId);
        
        return user.getIdentity_public_key();
    }

    public void blockUser(String blockerEmail, String blockedEmail) {
        User blocker = requireUserByEmail(blockerEmail);
        User blocked = requireUserByEmail(blockedEmail);

        if (blocker.getId().equals(blocked.getId())) {
            throw new IllegalArgumentException("User cannot block self");
        }

        if (!blockedUserRepo.existsByUserIdAndBlockedUserId(blocker.getId(), blocked.getId())) {
            blockedUserRepo.save(
                BlockedUser.builder()
                    .userId(blocker.getId())
                    .blockedUserId(blocked.getId())
                    .blocked_at(LocalDateTime.now())
                    .build()
            );
        }
    }

    public void unblockUser(String blockerEmail, String blockedEmail) {
        User blocker = requireUserByEmail(blockerEmail);
        User blocked = requireUserByEmail(blockedEmail);

        blockedUserRepo.deleteByUserIdAndBlockedUserId(blocker.getId(), blocked.getId());
    }

    private User requireUserById(String userId) {
        UUID userUuid = UUID.fromString(userId);
        User user = userRepo.findById(userUuid).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        return user;
    }

    private User requireUserByEmail(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User with email " + email + " not found");
        }
        return user;
    }

}
