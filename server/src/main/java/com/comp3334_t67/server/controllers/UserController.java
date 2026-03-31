package com.comp3334_t67.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.AllArgsConstructor;

import com.comp3334_t67.server.services.*;
import com.comp3334_t67.server.dtos.*;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    
    private final UserService userService;

    // ACCESS SELF INFO

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
}
