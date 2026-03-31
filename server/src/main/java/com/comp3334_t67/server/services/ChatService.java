package com.comp3334_t67.server.services;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;
import java.util.*;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ChatService {
    
    private final FriendChatRepository chatRepo;
    private final UserRepository userRepo;


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

    public void unblockFriend(UUID chatId, String unblockUserEmail) {
        UUID unblockUser = requireUserIdByEmail(unblockUserEmail);
        // find the friend chat by id
        Optional<FriendChat> optionalFriendChat = chatRepo.findById(chatId);

        if (optionalFriendChat.isPresent()) {
            FriendChat friendChat = optionalFriendChat.get();
            // check if the user is part of the friend chat
            if (friendChat.getUser1Id().equals(unblockUser) && friendChat.getBlockedId() == 1) {
                friendChat.setBlockedId(0); // unblock user1
            } else if (friendChat.getUser2Id().equals(unblockUser) && friendChat.getBlockedId() == 2) {
                friendChat.setBlockedId(0); // unblock user2
            }
            // save the updated friend chat to the database
            chatRepo.save(friendChat);
        }
    }

    // Remove a friend
    public void removeFriend(UUID chatId) {
        // find the friend chat by id
        Optional<FriendChat> optionalFriendChat = chatRepo.findById(chatId);

        if (optionalFriendChat.isPresent()) {
            FriendChat friendChat = optionalFriendChat.get();
            // delete the friend chat from the database
            chatRepo.delete(friendChat);
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

    
}
