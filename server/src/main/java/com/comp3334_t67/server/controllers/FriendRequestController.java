package com.comp3334_t67.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.*;

import com.comp3334_t67.server.Exceptions.OtpSessionMissingException;
import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.services.*;

import jakarta.servlet.http.*;
import java.time.Duration;
import java.util.*;

@RestController
@RequestMapping("/api/requests")
@AllArgsConstructor
public class FriendRequestController {

    private final int REQUEST_WINDOW = 10; // 10 minutes
    private final int MAX_REQUESTS_PER_WINDOW = 5;
    private final FriendRequestService friendService;
    private final RateLimitService rateLimitService;

    // Send friend request
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Void>> sendFriendRequest(@RequestBody FriendReqRequest requestDto, HttpSession session) {
        String senderEmail = (String) session.getAttribute("OTP_USER");
        String key = "friend-request:user:" + senderEmail;
        rateLimitService.assertAllowed(key, MAX_REQUESTS_PER_WINDOW, Duration.ofMinutes(REQUEST_WINDOW));

        friendService.sendFriendRequest(senderEmail, requestDto.getReceiverEmail());
        return ResponseEntity.ok(ApiResponse.success("Friend request sent successfully", null));
    }

    // Respond to friend request
    @PutMapping("/{requestId}/respond")
    public ResponseEntity<ApiResponse<Void>> respondToFriendRequest(@PathVariable String requestId, @RequestBody ActionRequest requestDto, HttpSession session) {
        String receiverEmail = (String) session.getAttribute("OTP_USER");
        friendService.respondToFriendRequest(receiverEmail, requestId, requestDto.isAccepted());
        return ResponseEntity.ok(ApiResponse.success("response sent successfully", null));
    }

    // Cancel friend request
    @DeleteMapping("/{requestId}")
    public ResponseEntity<ApiResponse<Void>> cancelFriendRequest(@PathVariable String requestId, HttpSession session) {
        String senderEmail = (String) session.getAttribute("OTP_USER");
        if (senderEmail == null) {
            throw new OtpSessionMissingException("No OTP verification in progress");
        }
        friendService.cancelFriendRequest(senderEmail, requestId);
        return ResponseEntity.ok(ApiResponse.success("Friend request cancelled successfully", null));
    }

    // get all incoming friend requests for user in session
    @GetMapping("/incoming")
    public ResponseEntity<ApiResponse<List<FriendRequestDto>>> getAllIncomingRequests(HttpSession session) {
        String receiverEmail = (String) session.getAttribute("OTP_USER");
        if (receiverEmail == null) {
            throw new OtpSessionMissingException("No OTP verification in progress");
        }
        return ResponseEntity.ok(ApiResponse.success("Incoming friend requests retrieved successfully", friendService.getIncomingFriendRequests(receiverEmail)));
    }

    // get all outgoing friend requests for user in session
    @GetMapping("/outgoing")
    public ResponseEntity<ApiResponse<List<FriendRequestDto>>> getAllOutgoingRequests(HttpSession session) {
        String senderEmail = (String) session.getAttribute("OTP_USER");
        if (senderEmail == null) {
            throw new OtpSessionMissingException("No OTP verification in progress");
        }
        return ResponseEntity.ok(ApiResponse.success("Outgoing friend requests retrieved successfully", friendService.getOutgoingFriendRequests(senderEmail)));
    }

}
