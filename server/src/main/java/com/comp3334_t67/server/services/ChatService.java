package com.comp3334_t67.server.services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.comp3334_t67.server.Exceptions.ChatMembershipException;
import com.comp3334_t67.server.Exceptions.ChatNotFoundException;
import com.comp3334_t67.server.Exceptions.MessageValidationException;
import com.comp3334_t67.server.Exceptions.MessagingBlockedException;
import com.comp3334_t67.server.Exceptions.UserNotFoundException;
import com.comp3334_t67.server.Exceptions.UsersNotFriendsException;
import com.comp3334_t67.server.enums.FriendRequestStatus;
import com.comp3334_t67.server.enums.MessageStatus;
import com.comp3334_t67.server.dtos.FriendChatDto;
import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;
import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ChatService {

    private static final int MAX_MESSAGE_HASH_LENGTH = 4096;
    private static final int MAX_NONCE_LENGTH = 512;
    private static final String HASH_FORMAT_REGEX = "^[A-Za-z0-9+/=._:-]+$";
    
    private final FriendChatRepository chatRepo;
    private final FriendRequestRepository requestRepo;
    private final MessageRepository messageRepo;
    private final BlockedUserRepository blockedUserRepo;
    private final UserRepository userRepo;

    // send message
    public void sendMessage(String chatId, String senderEmail, String content, String nouce) {
        validateSendMessageInput(chatId, content, nouce);

        UUID senderId = requireUserIdByEmail(senderEmail);
        FriendChat chat = chatRepo.findById(UUID.fromString(chatId))
            .orElseThrow(() -> new ChatNotFoundException("Chat not found"));

        UUID receiverId = resolveCounterparty(chat, senderId);

        if (!areAcceptedFriends(senderId, receiverId)) {
            throw new UsersNotFriendsException("Users are not accepted friends");
        }

        if (isBlocked(senderId, receiverId)) {
            throw new MessagingBlockedException("Messaging is blocked between these users");
        }

        Message message = Message.builder()
            .chatId(chat.getId())
            .senderId(senderId)
            .receiverId(receiverId)
            .content_hashed(content)
            .nouce(nouce)
            .status(MessageStatus.SENT)
            .created_at(LocalDateTime.now())
            .build();

        messageRepo.save(message);
    }

    // Remove a friend
    public void removeFriend(String chatId, String requesterEmail) {
        UUID requesterId = requireUserIdByEmail(requesterEmail);

        // find the friend chat by id
        Optional<FriendChat> optionalFriendChat = chatRepo.findById(UUID.fromString(chatId));

        if (optionalFriendChat.isPresent()) {
            FriendChat friendChat = optionalFriendChat.get();

            if (!friendChat.getUser1Id().equals(requesterId) && !friendChat.getUser2Id().equals(requesterId)) {
                throw new ChatMembershipException("User does not belong to this chat");
            }

            // delete the friend chat from the database
            chatRepo.delete(friendChat);
        }
    }

    // Get all friend chats for a user
    public List<FriendChatDto> getFriendChats(String userEmail) {

        // get user id by email, throw exception if user not found
        UUID userId = requireUserIdByEmail(userEmail);

        // get all blocked users by user
        Set<UUID> blockedUserIds = blockedUserRepo.findByUserId(userId)
            .stream()
            .map(BlockedUser::getBlockedUserId)
            .collect(Collectors.toSet());

        // result = list of friend chat dtos to return
        List<FriendChatDto> result = new ArrayList<>();
        // get all friend chats involving the user
        List<FriendChat> chats = chatRepo.findAllByUserId(userId);

        // loop through each chat and construct the dto
        for (FriendChat chat : chats) {

            // get other user id from chat
            UUID counterpartyId = resolveCounterparty(chat, userId);

            // Ignore chats that current user has blocked.
            if (blockedUserIds.contains(counterpartyId)) {
                continue;
            }
            
            // Get all unread messages for the user in the chat
            List<Message> unreadMessages = messageRepo.findByReceiverIdAndChatIdAndStatus(userId, chat.getId(), MessageStatus.SENT);
            // count number of unread messages
            long unreadCount = unreadMessages.size();

            // Update status of unread messages to "delivered"
            if (!unreadMessages.isEmpty()) {
                LocalDateTime deliveredAt = LocalDateTime.now();
                for (Message message : unreadMessages) {
                    message.setStatus(MessageStatus.DELIVERED);
                    message.setDelivered_at(deliveredAt);
                }
                messageRepo.saveAll(unreadMessages);
            }

            // Get timestamp of the last message in the chat, or use chat creation time if no messages
            LocalDateTime lastMessageDateTime = messageRepo.findTopByChatIdOrderByCreated_atDesc(chat.getId())
                .map(Message::getCreated_at)
                .orElse(chat.getCreated_at());

            result.add(
                FriendChatDto.builder()
                    .senderId(counterpartyId)
                    .receiverId(userId)
                    .numOfUnreadMessage(unreadCount)
                    .lastMessageDateTime(lastMessageDateTime)
                    .build()
            );
        }

        // sort result by last message timestamp in descending order, with nulls (no messages) last
        result.sort(Comparator.comparing(FriendChatDto::getLastMessageDateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        return result;
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
            throw new UserNotFoundException("User with email " + email + " not found");
        }
        return user.getId();
    }

    private UUID resolveCounterparty(FriendChat chat, UUID senderId) {
        if (chat.getUser1Id().equals(senderId)) {
            return chat.getUser2Id();
        }
        if (chat.getUser2Id().equals(senderId)) {
            return chat.getUser1Id();
        }
        throw new ChatMembershipException("Sender does not belong to the chat");
    }

    private boolean areAcceptedFriends(UUID userA, UUID userB) {
        boolean hasChat = chatRepo.findByUsers(userA, userB).isPresent();
        boolean hasAcceptedRequest = requestRepo.existsBySenderIdAndReceiverIdAndStatus(userA, userB, FriendRequestStatus.ACCEPTED)
            || requestRepo.existsBySenderIdAndReceiverIdAndStatus(userB, userA, FriendRequestStatus.ACCEPTED);

        return hasChat && hasAcceptedRequest;
    }

    private boolean isBlocked(UUID userA, UUID userB) {
        return blockedUserRepo.existsByUserIdAndBlockedUserId(userA, userB)
            || blockedUserRepo.existsByUserIdAndBlockedUserId(userB, userA);
    }

    private void validateSendMessageInput(String chatId, String content, String nouce) {
        try {
            UUID.fromString(chatId);
        } catch (IllegalArgumentException ex) {
            throw new MessageValidationException("Invalid chatId format");
        }

        if (content == null || content.isBlank()) {
            throw new MessageValidationException("Message content cannot be empty");
        }

        if (content.length() > MAX_MESSAGE_HASH_LENGTH) {
            throw new MessageValidationException("Message content exceeds size limit");
        }

        if (!content.matches(HASH_FORMAT_REGEX)) {
            throw new MessageValidationException("Malformed message content format");
        }

        if (nouce == null || nouce.isBlank()) {
            throw new MessageValidationException("Nonce cannot be empty");
        }

        if (nouce.length() > MAX_NONCE_LENGTH) {
            throw new MessageValidationException("Nonce exceeds size limit");
        }
    }

    
}
