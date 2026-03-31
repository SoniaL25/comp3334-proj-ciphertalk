package com.comp3334_t67.server.controllers;

import org.springframework.web.bind.annotation.*;
import lombok.*;

import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.services.*;

import jakarta.servlet.http.*;
import java.util.*;



@RestController
@RequestMapping("/api/friends")
@AllArgsConstructor
public class FriendController {

    private final FriendRequestService friendService;

    @PostMapping("/requests")
    public void sendFriendRequest(@RequestBody FriendReqRequest requestDto) {
        friendService.sendFriendRequest(requestDto.getSenderEmail(), requestDto.getReceiverEmail());
    }

    @PutMapping("/requests/{requestId}/respond")
    public void respondToFriendRequest(@PathVariable String requestId, @RequestBody ActionRequest requestDto) {
        friendService.respondToFriendRequest(requestId, requestDto.isAccepted());
    }

    @PutMapping("/requests/{requestId}/cancel")
    public void cancelFriendRequest(@PathVariable String requestId, HttpSession session) {
        String senderEmail = (String) session.getAttribute("email");
        if (senderEmail == null) {
            throw new IllegalStateException("No OTP verification in progress");
        }
        friendService.cancelFriendRequest(senderEmail, requestId);
    }

    @GetMapping("/requests/incoming")
    public List<FriendRequestDto> getAllIncomingRequests(HttpSession session) {
        String receiverEmail = (String) session.getAttribute("email");
        if (receiverEmail == null) {
            throw new IllegalStateException("No OTP verification in progress");
        }
        return friendService.getIncomingFriendRequests(receiverEmail);
    }

    @GetMapping("/requests/outgoing")
    public List<FriendRequestDto> getAllOutgoingRequests(HttpSession session) {
        String senderEmail = (String) session.getAttribute("email");
        if (senderEmail == null) {
            throw new IllegalStateException("No OTP verification in progress");
        }
        return friendService.getOutgoingFriendRequests(senderEmail);
    }

}
