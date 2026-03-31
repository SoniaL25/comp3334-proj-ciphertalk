package com.comp3334_t67.server.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    
    private final UserRepository userRepo;

    public void uploadPublicKey(String userId, String publicKey) {
        User user = requireUserById(userId);
        
        user.setIdentity_public_key(publicKey);
        userRepo.save(user);
    }

    public String getPublicKey(String userId) {
        User user = requireUserById(userId);
        
        return user.getIdentity_public_key();
    }

    private User requireUserById(String userId) {
        UUID userUuid = UUID.fromString(userId);
        User user = userRepo.findById(userUuid).orElse(null);
        if (user == null) {
            throw new RuntimeException("User not found with ID: " + userId);
        }
        return user;
    }


}
