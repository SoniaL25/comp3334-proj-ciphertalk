package com.comp3334_t67.server.services;

import java.time.*;
import java.util.*;

import com.comp3334_t67.server.Exceptions.FriendRequestNotFoundException;
import com.comp3334_t67.server.Exceptions.FriendRequestOwnershipException;
import com.comp3334_t67.server.Exceptions.FriendRequestStateException;
import com.comp3334_t67.server.Exceptions.UserNotFoundException;
import com.comp3334_t67.server.dtos.FriendRequestDto;
import com.comp3334_t67.server.enums.*;
import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FriendRequestService {

    private final FriendRequestRepository requestRepo;
    private final BlockedUserRepository blockedUserRepo;
    private final FriendChatRepository chatRepo;
    private final UserRepository userRepo;

    // send friend request
    public void sendFriendRequest(String senderEmail, String receiverEmail) {
        UUID senderId = requireUserIdByEmail(senderEmail);
        UUID receiverId = requireUserIdByEmail(receiverEmail);

        // Check if sender is block by receiver
        if (blockedUserRepo.existsByUserIdAndBlockedUserId(receiverId, senderId)) {
            throw new FriendRequestStateException("Cannot send friend request: you are blocked by the user");
        }

        // Check if there is already a pending friend request from sender to receiver
        if (requestRepo.existsBySenderIdAndReceiverIdAndStatus(senderId, receiverId, FriendRequestStatus.PENDING)) {
            throw new FriendRequestStateException("Friend request already sent and pending");
        }

        // create a new friend request with status PENDING
        FriendRequest friendRequest = createFriendRequest(senderId, receiverId);
        // save the friend request to the database
        requestRepo.save(friendRequest);
    }

    // Reponse to friend request
    public void respondToFriendRequest(String receiverEmail, String requestId, boolean accept) {
        // find the friend request by id
        FriendRequest friendRequest = requestRepo.findById(UUID.fromString(requestId))
            .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found"));

        // only allow response if the request is still pending
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new FriendRequestStateException("Friend request has already been responded to");
        }

        // check if the receiverId matches the receiverId of the friend request
        if (!Objects.equals(friendRequest.getReceiverId(), requireUserIdByEmail(receiverEmail))) {
            throw new FriendRequestOwnershipException("Only the receiver can respond to this friend request");
        }

        // update the status of the friend request based on the response
        if (accept) {
            friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
            friendRequest.setRespondedAt(LocalDateTime.now());
            // add each other to friends list (not implemented here)
            FriendChat friendChat = createFriendChat(friendRequest.getSenderId(), friendRequest.getReceiverId());
            chatRepo.save(friendChat);

        } else {
            friendRequest.setStatus(FriendRequestStatus.REJECTED);
            friendRequest.setRespondedAt(LocalDateTime.now());
        }

        // save the updated friend request to the database
        requestRepo.save(friendRequest);
    }

    // Cancel friend request
    public void cancelFriendRequest(String senderEmail, String requestId) {
        UUID senderId = requireUserIdByEmail(senderEmail);
        // find friend request by id
        FriendRequest friendRequest = requestRepo.findById(UUID.fromString(requestId))
            .orElseThrow(() -> new FriendRequestNotFoundException("Friend request not found"));

        // only allow cancellation if the request is still pending
        if (friendRequest.getStatus() != FriendRequestStatus.PENDING) {
            throw new FriendRequestStateException("Friend request has already been responded to");
        }

        // check if the senderId matches the senderId of the friend request
        if (!Objects.equals(friendRequest.getSenderId(), senderId)) {
            throw new FriendRequestOwnershipException("Only the sender can cancel this friend request");
        }

        // update the status of the friend request to CANCELED
        friendRequest.setStatus(FriendRequestStatus.CANCELED);
        friendRequest.setRespondedAt(LocalDateTime.now());
        // save the updated friend request to the database
        requestRepo.save(friendRequest);
    }

    // Get all incoming friend requests
    public List<FriendRequestDto> getIncomingFriendRequests(String userEmail) {
        UUID userId = requireUserIdByEmail(userEmail);
        // find all friend requests where the receiver is the user and status is PENDING
        List<FriendRequest> friendRequests = requestRepo.findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);
        List<FriendRequestDto> friendRequestDtos = new ArrayList<>();
        for (FriendRequest friendRequest : friendRequests) {
            friendRequestDtos.add(convertToDto(friendRequest));
        }
        return friendRequestDtos;
    }

    // Get all outgoing friend requests
    public List<FriendRequestDto> getOutgoingFriendRequests(String userEmail) {
        UUID userId = requireUserIdByEmail(userEmail);
        // find all friend requests where the sender is the user and status is PENDING
        List<FriendRequest> friendRequests = requestRepo.findBySenderIdAndStatus(userId, FriendRequestStatus.PENDING);
        List<FriendRequestDto> friendRequestDtos = new ArrayList<>();
        for (FriendRequest friendRequest : friendRequests) {
            friendRequestDtos.add(convertToDto(friendRequest));
        }
        return friendRequestDtos;
    }

    
    // HELPER METHODS ============================

    // Get user id by email, throw exception if user not found
    private UUID requireUserIdByEmail(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User with email " + email + " not found");
        }
        return user.getId();
    }

    // Create new friend request
    private FriendRequest createFriendRequest(UUID senderId, UUID receiverId) {
        FriendRequest friendRequest = FriendRequest.builder()
            .senderId(senderId)
            .receiverId(receiverId)
            .status(FriendRequestStatus.PENDING)
            .createdAt(LocalDateTime.now())
            .respondedAt(null)
            .build();
            
        return friendRequest;
    }

    // Create new friend chat
    private FriendChat createFriendChat(UUID user1Id, UUID user2Id) {
        FriendChat friendChat = FriendChat.builder()
            .user1Id(user1Id)
            .user2Id(user2Id)
            .createdAt(LocalDateTime.now())
            .build();
        return chatRepo.save(friendChat);
    }

    // Convert FriendRequest to FriendRequestDto
    private FriendRequestDto convertToDto(FriendRequest friendRequest) {
        FriendRequestDto dto = new FriendRequestDto();
        dto.setId(friendRequest.getId());
        dto.setSenderId(friendRequest.getSenderId());
        dto.setSenderEmail(requireUserEmailById(friendRequest.getSenderId()));
        dto.setReceiverId(friendRequest.getReceiverId());
        dto.setStatus(friendRequest.getStatus());
        dto.setCreatedAt(friendRequest.getCreatedAt());
        return dto;
    }

    private String requireUserEmailById(UUID userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new UserNotFoundException("User with id " + userId + " not found");
        }
        return user.getEmail();
    }

}
