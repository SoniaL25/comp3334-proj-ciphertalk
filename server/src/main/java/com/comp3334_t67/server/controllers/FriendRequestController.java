package com.comp3334_t67.server.controllers;

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

    private final int RATE_LIMIT_WINDOW = 5;
    private final int RATE_LIMIT_LIMIT = 10;

    private final FriendRequestService friendService;
    private final RateLimitService rateLimitService;

    @PostMapping("/send")
    public void sendFriendRequest(@RequestBody FriendReqRequest requestDto) {
        String key = "friend-request:" + requestDto.getSenderEmail();
        rateLimitService.assertAllowed(key, RATE_LIMIT_LIMIT, Duration.ofMinutes(RATE_LIMIT_WINDOW));

        friendService.sendFriendRequest(requestDto.getSenderEmail(), requestDto.getReceiverEmail());
    }

    @PutMapping("/{requestId}/respond")
    public void respondToFriendRequest(@PathVariable String requestId, @RequestBody ActionRequest requestDto) {
        friendService.respondToFriendRequest(requestId, requestDto.isAccepted());
    }

    @PutMapping("/{requestId}/cancel")
    public void cancelFriendRequest(@PathVariable String requestId, HttpSession session) {
        String senderEmail = (String) session.getAttribute("email");
        if (senderEmail == null) {
            throw new OtpSessionMissingException("No OTP verification in progress");
        }
        friendService.cancelFriendRequest(senderEmail, requestId);
    }

    @GetMapping("/incoming")
    public List<FriendRequestDto> getAllIncomingRequests(HttpSession session) {
        String receiverEmail = (String) session.getAttribute("email");
        if (receiverEmail == null) {
            throw new OtpSessionMissingException("No OTP verification in progress");
        }
        return friendService.getIncomingFriendRequests(receiverEmail);
    }

    @GetMapping("/outgoing")
    public List<FriendRequestDto> getAllOutgoingRequests(HttpSession session) {
        String senderEmail = (String) session.getAttribute("email");
        if (senderEmail == null) {
            throw new OtpSessionMissingException("No OTP verification in progress");
        }
        return friendService.getOutgoingFriendRequests(senderEmail);
    }

}
