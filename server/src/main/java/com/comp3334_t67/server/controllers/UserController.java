package com.comp3334_t67.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.AllArgsConstructor;

import com.comp3334_t67.server.services.*;
import com.comp3334_t67.server.dtos.*;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    
    private final UserService userService;

    // ACCESS SELF INFO

    @GetMapping("/{user-id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserInfo(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("User info retrieved successfully", userService.getUserInfoById(userId)));
    }

    @PostMapping("/{user-id}/public-key")
    public ResponseEntity<ApiResponse<Void>> uploadPublicKey(@PathVariable String userId, @RequestBody UploadPublicKeyRequest request) {
        userService.uploadPublicKey(userId, request.getPublicKey());
        return ResponseEntity.ok(ApiResponse.success("Public key uploaded successfully", null));
    }

    // ACCESS OTHER USER INFO

    @GetMapping("/public-keys/{user-id}") // TODO: need more security
    public ResponseEntity<ApiResponse<String>> getPublicKey(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.success("Public key retrieved successfully", userService.getPublicKey(userId)));
    }

    @PostMapping("/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(@RequestParam String blockedEmail, HttpSession session) {
        String blockerEmail = requireSessionEmail(session);
        userService.blockUser(blockerEmail, blockedEmail);
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", null));
    }

    @PostMapping("/unblock")
    public ResponseEntity<ApiResponse<Void>> unblockUser(@RequestParam String blockedEmail, HttpSession session) {
        String blockerEmail = requireSessionEmail(session);
        userService.unblockUser(blockerEmail, blockedEmail);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", null));
    }

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
