package com.comp3334_t67.server.services;

import java.time.*;
import java.util.*;

import com.comp3334_t67.server.enums.*;
import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FriendService {

    // TODO: add logging and error handling

    private final FriendRequestRepository requestRepo;
    private final FriendChatRepository chatRepo;
    private final UserRepository userRepo;

    // send friend request
    public void sendFriendRequest(String senderEmail, String receiverEmail) {
        UUID senderId = requireUserIdByEmail(senderEmail);
        UUID receiverId = requireUserIdByEmail(receiverEmail);

        // create a new friend request with status PENDING
        FriendRequest friendRequest = createFriendRequest(senderId, receiverId);
        // save the friend request to the database
        requestRepo.save(friendRequest);
    }

    // Reponse to friend request
    public void respondToFriendRequest(UUID requestId, boolean accept) {
        // find the friend request by id
        Optional<FriendRequest> optionalFriendRequest = requestRepo.findById(requestId);

        if (optionalFriendRequest.isPresent() && optionalFriendRequest.get().getStatus() == FriendRequestStatus.PENDING) {
            FriendRequest friendRequest = optionalFriendRequest.get();
            // update the status of the friend request based on the response
            if (accept) {
                friendRequest.setStatus(FriendRequestStatus.ACCEPTED);
                friendRequest.setResponded_at(LocalDateTime.now());
                // add each other to friends list (not implemented here)
                FriendChat friendChat = createFriendChat(friendRequest.getSenderId(), friendRequest.getReceiverId());
                chatRepo.save(friendChat);

            } else {
                friendRequest.setStatus(FriendRequestStatus.REJECTED);
            }
            // save the updated friend request to the database
            requestRepo.save(friendRequest);
        }
    }

    // Cancel friend request
    public void cancelFriendRequest(String senderEmail, UUID requestId) {
        UUID senderId = requireUserIdByEmail(senderEmail);
        // find friend request by id
        Optional<FriendRequest> optionalFriendRequest = requestRepo.findById(requestId);
        if (optionalFriendRequest.isPresent() && optionalFriendRequest.get().getStatus() == FriendRequestStatus.PENDING) {
            FriendRequest friendRequest = optionalFriendRequest.get();
            // check if the senderId matches the senderId of the friend request
            if (friendRequest.getSenderId().equals(senderId)) {
                // update the status of the friend request to CANCELED
                friendRequest.setStatus(FriendRequestStatus.CANCELED);
                // save the updated friend request to the database
                requestRepo.save(friendRequest);
            }
        }
    }

    // Get all incoming friend requests
    public List<FriendRequest> getIncomingFriendRequests(String userEmail) {
        UUID userId = requireUserIdByEmail(userEmail);
        // find all friend requests where the receiver is the user and status is PENDING
        return requestRepo.findByReceiverIdAndStatus(userId, FriendRequestStatus.PENDING);
    }

    // Get all outgoing friend requests
    public List<FriendRequest> getOutgoingFriendRequests(String userEmail) {
        UUID userId = requireUserIdByEmail(userEmail);
        // find all friend requests where the sender is the user and status is PENDING
        return requestRepo.findBySenderIdAndStatus(userId, FriendRequestStatus.PENDING);
    }

    // Block a friend
    public void blockFriend(UUID chatId, String blockUserEmail) {
        UUID blockUser = requireUserIdByEmail(blockUserEmail);
        // find the friend chat by id
        Optional<FriendChat> optionalFriendChat = chatRepo.findById(chatId);

        if (optionalFriendChat.isPresent()) {
            FriendChat friendChat = optionalFriendChat.get();
            // check if the user is part of the friend chat
            if (friendChat.getUser1Id().equals(blockUser)) {
                friendChat.setBlockedId(1); // block user1
            } else if (friendChat.getUser2Id().equals(blockUser)) {
                friendChat.setBlockedId(2); // block user2
            }
            // save the updated friend chat to the database
            chatRepo.save(friendChat);
        }
    }

    // Get all friend chats for a user
    public List<FriendChat> getFriendChats(String userEmail) {
        UUID userId = requireUserIdByEmail(userEmail);
        // find all friend chats where the user is either user1 or user2
        return chatRepo.findAllByUserId(userId);
    }

    // Check if two users are friends
    public boolean areFriends(String userEmail1, String userEmail2) {
        UUID userId1 = requireUserIdByEmail(userEmail1);
        UUID userId2 = requireUserIdByEmail(userEmail2);
        // check if there is a friend chat between the two users
        FriendChat friendChat = chatRepo.findByUser1IdAndUser2Id(userId1, userId2);
        return friendChat != null;
    }

    // HELPER METHODS ============================

    // Get user id by email, throw exception if user not found
    private UUID requireUserIdByEmail(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User with email " + email + " not found");
        }
        return user.getId();
    }

    // Create new friend request
    private FriendRequest createFriendRequest(UUID senderId, UUID receiverId) {
        FriendRequest friendRequest = FriendRequest.builder()
            .senderId(senderId)
            .receiverId(receiverId)
            .status(FriendRequestStatus.PENDING)
            .created_at(LocalDateTime.now())
            .responded_at(null)
            .build();
            
        return friendRequest;
    }

    // Create new friend chat
    private FriendChat createFriendChat(UUID user1Id, UUID user2Id) {
        FriendChat friendChat = FriendChat.builder()
            .user1Id(user1Id)
            .user2Id(user2Id)
            .created_at(LocalDateTime.now())
            .blockedId(0)
            .build();
        return chatRepo.save(friendChat);
    }

}

// Chat chat = executeWithHandling(() -> {
//     return chatRepo.findById(id)
//         .orElseThrow(() -> new ChatNotFoundException("Chat not found"));
// });

// public <T> T executeWithHandling(
//     Supplier<T> action,
//     Function<Exception, RuntimeException> errorHandler
// ) {
//     try {
//         return action.get();
//     } catch (Exception e) {
//         throw errorHandler.apply(e);
//     }
// }

// Chat chat = executeWithHandling(
//     () -> chatRepo.findById(id).orElseThrow(),
//     e -> new ChatNotFoundException("Chat not found: " + id)
// );