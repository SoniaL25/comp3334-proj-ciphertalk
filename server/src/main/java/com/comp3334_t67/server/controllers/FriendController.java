package com.comp3334_t67.server.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.services.FriendService;


@RestController
@RequestMapping("/api/friends")
@AllArgsConstructor
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/requests")
    public void sendFriendRequest(@RequestBody FriendRequestDto requestDto) {
        friendService.sendFriendRequest(requestDto.getSenderEmail(), requestDto.getReceiverEmail());
    }

    @PostMapping("/requests/respond")
    public void respondToFriendRequest(@RequestBody FriendRequestResponseDto requestDto) {
        friendService.respondToFriendRequest(requestDto.getRequestId(), requestDto.isAccepted());
    }

    @PostMapping("/requests/cancel")
    public void cancelFriendRequest(@RequestBody CancelFriendRequestDto requestDto) {
        friendService.cancelFriendRequest(requestDto.getSenderEmail(), requestDto.getRequestId());
    }

    // TODO: change both
    // @GetMapping("/requests/incoming")
    // public List<FriendRequestDto> getAllIncomingRequests(@RequestBody String receiverEmail) {
    //     return friendService.getAllIncomingRequests(receiverEmail);
    // }

    // @GetMapping("/requests/outgoing")
    // public List<FriendRequestDto> getAllOutgoingRequests(@RequestBody String senderEmail) {
    //     return friendService.getAllOutgoingRequests(senderEmail);
    // }
    
    


}
