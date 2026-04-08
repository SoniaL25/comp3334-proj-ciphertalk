package com.comp3334_t67.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.AllArgsConstructor;

import com.comp3334_t67.server.services.*;
import com.comp3334_t67.server.dtos.*;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;

@RestController
@RequestMapping("/api")
@AllArgsConstructor
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    private final UserService userService;
    private final RateLimitService rateLimitService;

    private final int KEY_MAX_UPLOADS = 3; // e.g. max 3 public key uploads per 5 minutes
    private final Duration KEY_UPLOAD_DURATION = Duration.ofMinutes(5);

    // ACCESS SELF INFO

    // Get profile of user in session
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDto>> getProfile(HttpSession session) {
        String email = requireSessionEmail(session);
        log.info("Profile request received for user={}", email);
        UserDto profile = userService.getUserInfoByEmail(email);
        log.info("Profile request succeeded for user={}", email);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    // get user in session's public key
    @GetMapping("/profile/public-key")
    public ResponseEntity<ApiResponse<KeyDto>> getPublicKey(HttpSession session) {
        String email = requireSessionEmail(session);
        log.info("Own public key request received for user={}", email);
        KeyDto key = userService.getPublicKeyByEmail(email);
        log.info("Own public key request succeeded for user={}", email);
        return ResponseEntity.ok(ApiResponse.success("Public key retrieved successfully", key));
    }

    // upload user in session's public key
    @PutMapping("/profile/public-key")
    public ResponseEntity<ApiResponse<Void>> uploadPublicKey(@RequestBody UploadPublicKeyRequest request, HttpSession session) {
        String email = requireSessionEmail(session);
        log.info("Public key upload request received for user={}", email);
        
        String key = "upload-public-key:user:" + email;
        rateLimitService.assertAllowed(key, KEY_MAX_UPLOADS, KEY_UPLOAD_DURATION);

        userService.uploadPublicKey(email, request.getPublicKey());
        log.info("Public key upload succeeded for user={}", email);
        return ResponseEntity.ok(ApiResponse.success("Public key uploaded successfully", null));
    }
    

    // ACCESS OTHER USER INFO

    // get other user's info by their user ID
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> getUserInfo(@PathVariable String userId) {
        log.info("User info request received for userId={}", userId);
        UserDto info = userService.getUserInfoById(userId);
        log.info("User info request succeeded for userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("User info retrieved successfully", info));
    }

    // get other user's public key by their user ID
    @GetMapping("/users/{userId}/public-key")
    public ResponseEntity<ApiResponse<KeyDto>> getPublicKey(@PathVariable String userId) {
        log.info("Peer public key request received for userId={}", userId);
        KeyDto key = userService.getPublicKeyById(userId);
        log.info("Peer public key request succeeded for userId={}", userId);
        return ResponseEntity.ok(ApiResponse.success("Public key retrieved successfully", key));
    }

    // get other user's public key by email
    @GetMapping("/users/email/{email}/public-key")
    public ResponseEntity<ApiResponse<KeyDto>> getPublicKeyByEmail(@PathVariable String email) {
        log.info("Peer public key request received for email={}", email);
        KeyDto key = userService.getPublicKeyByEmail(email);
        log.info("Peer public key request succeeded for email={}", email);
        return ResponseEntity.ok(ApiResponse.success("Public key retrieved successfully", key));
    }


    // block other users
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@PathVariable String userId, HttpSession session) {
        String blockerEmail = requireSessionEmail(session);
        log.info("Block request received from user={} for targetId={}", blockerEmail, userId);
        userService.blockUser(blockerEmail, userId);
        log.info("Block request succeeded from user={} for targetId={}", blockerEmail, userId);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    // unblock other users
    @DeleteMapping("/users/{userId}/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@PathVariable String userId, HttpSession session) {
        String blockerEmail = requireSessionEmail(session);
        log.info("Unblock request received from user={} for targetId={}", blockerEmail, userId);
        userService.unblockUser(blockerEmail, userId);
        log.info("Unblock request succeeded from user={} for targetId={}", blockerEmail, userId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }


    // HELPER METHODS

    // retrieve email from session
    private String requireSessionEmail(HttpSession session) {
        if (session == null) {
            throw new IllegalStateException("No active session");
        }

        String email = (String) session.getAttribute("OTP_USER");
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("No authenticated user in session");
        }

        return email;
    }
}
