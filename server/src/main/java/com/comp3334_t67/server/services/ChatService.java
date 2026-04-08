package com.comp3334_t67.server.services;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.comp3334_t67.server.Exceptions.*;
import com.comp3334_t67.server.enums.*;
import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;
import java.util.*;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class ChatService {

    private static final int MAX_MESSAGE_HASH_LENGTH = 4096; // 4KB hash size limit
    private static final int MAX_NONCE_LENGTH = 64; // base64 nonce string length upper bound
    private static final String HASH_FORMAT_REGEX = "^[A-Za-z0-9+/=._:-]+$";
    
    private final FriendChatRepository chatRepo;
    private final FriendRequestRepository requestRepo;
    private final MessageRepository messageRepo;
    private final BlockedUserRepository blockedUserRepo;
    private final UserRepository userRepo;

    
    // Get all friend chats for a user
    public List<FriendChatDto> getFriendChats(String userEmail) {

        // get user id, blocked user ids, and all chats for the user
        UUID userId = requireUserIdByEmail(userEmail);
        // collect blocked user ids for quick lookup
        Set<UUID> blockedUserIds = getBlockedUserIds(userId);
        // load all chats where the user is either user1 or user2
        List<FriendChat> chats = chatRepo.findAllByUserId(userId);

        // build response dtos, skipping blocked chats
        List<FriendChatDto> result = new ArrayList<>();
        for (FriendChat chat : chats) {
            FriendChatDto dto = buildFriendChatDto(chat, userId, blockedUserIds);
            if (dto != null) {
                result.add(dto);
            }
        }

        // Keep newest chats first.
        result.sort(Comparator.comparing(FriendChatDto::getLastMessageDateTime, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return result;
    }

    // send message
    public void sendMessage(String chatId, String senderEmail, String content, String nonce, String clientMessageId, String tag, int ttlMinutes) {
        
        // validate input parameters
        validateSendMessageInput(chatId, content, nonce, clientMessageId, tag);

        UUID senderId = requireUserIdByEmail(senderEmail);
        FriendChat chat = chatRepo.findById(UUID.fromString(chatId))
            .orElseThrow(() -> new ChatNotFoundException("Chat not found"));

        // check for resend messages
        if (messageRepo.existsByClientMessageIdAndSenderIdAndChatId(clientMessageId, requireUserIdByEmail(senderEmail), UUID.fromString(chatId))) {
            throw new DuplicateMessageException("A message with the same clientMessageId has already been sent in this chat by the sender");
        }

        // check 1: chat belongs to sender
        if (!chat.getUser1Id().equals(senderId) && !chat.getUser2Id().equals(senderId)) {
            throw new ChatMembershipException("Sender does not belong to the chat");
        }

        UUID receiverId = resolveCounterparty(chat, senderId);

        // check 2: sender and receiver are accepted friends
        if (!areAcceptedFriends(senderId, receiverId)) {
            throw new UsersNotFriendsException("Users are not accepted friends");
        }

        // check 3: sender and receiver have not blocked each other
        if (isBlocked(senderId, receiverId)) {
            throw new MessagingBlockedException("Messaging is blocked between these users");
        }

        // if all validations pass, create and save the message
        messageRepo.save(createMessage(chat.getId(), senderId, receiverId, content, nonce, clientMessageId, tag, ttlMinutes));
    }

    // Get unread messages for a receiver in one chat
    public List<MessageDto> getUnreadMessagesForReceiver(String receiverEmail, String chatId) {
        UUID receiverId = requireUserIdByEmail(receiverEmail);
        UUID chatUuid = UUID.fromString(chatId);

        // fetch unread messages for this chat
        List<Message> unreadMessages = messageRepo.findByReceiverIdAndChatIdAndStatus(receiverId, chatUuid, MessageStatus.SENT);

        // exclude expired messages from delivery/results
        unreadMessages.removeIf(message -> message.getExpiresAt() != null && message.getExpiresAt().isBefore(LocalDateTime.now()));

        // mark them as delivered before returning them
        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.DELIVERED);
            messageRepo.save(message);
        }

        return unreadMessages.stream()
            .map(this::toMessageDto)
            .toList();
    }

    // Get all undelivered (SENT) messages for receiver across all chats
    public List<MessageDto> getUndeliveredMessagesForReceiver(String receiverEmail) {
        UUID receiverId = requireUserIdByEmail(receiverEmail);
        List<Message> undelivered = messageRepo.findByReceiverIdAndStatusOrderByCreatedAtAsc(receiverId, MessageStatus.SENT);

        // Exclude expired messages from inbox results.
        undelivered.removeIf(message -> message.getExpiresAt() != null && message.getExpiresAt().isBefore(LocalDateTime.now()));

        return undelivered.stream()
            .map(this::toMessageDto)
            .toList();
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

     // HELPER METHODS ============================

    // Collect blocked user ids for quick lookup.
    private Set<UUID> getBlockedUserIds(UUID userId) {
        return blockedUserRepo.findByUserId(userId)
            .stream()
            .map(BlockedUser::getBlockedUserId)
            .collect(Collectors.toSet());
    }

    // Build one chat summary, or skip it if blocked.
    private FriendChatDto buildFriendChatDto(FriendChat chat, UUID userId, Set<UUID> blockedUserIds) {
        UUID counterpartyId = resolveCounterparty(chat, userId);

        if (blockedUserIds.contains(counterpartyId)) {
            return null;
        }

        List<Message> unreadMessages = getUnreadMessages(userId, chat.getId());

        long unreadCount = unreadMessages.size();

        return FriendChatDto.builder()
            .chatId(chat.getId())
            .senderId(counterpartyId)
            .receiverId(userId)
            .friendEmail(requireUserEmailById(counterpartyId))
            .numOfUnreadMessage(unreadCount)
            .lastMessageDateTime(getLastMessageTime(chat))
            .build();
    }

    // Load unread messages for a chat.
    private List<Message> getUnreadMessages(UUID userId, UUID chatId) {
        List<Message> unreadMessages = messageRepo.findByReceiverIdAndChatIdAndStatus(userId, chatId, MessageStatus.SENT);

        // exclude expired messages
        unreadMessages.removeIf(message -> message.getExpiresAt() != null && message.getExpiresAt().isBefore(LocalDateTime.now()));

        return unreadMessages;

    }

    // Mark unread messages as delivered.
    private void markMessagesDelivered(List<Message> unreadMessages) {
        if (unreadMessages.isEmpty()) {
            return;
        }

        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.DELIVERED);
        }
        messageRepo.saveAll(unreadMessages);
    }

    // Use the latest message time, or the chat time if empty.
    private LocalDateTime getLastMessageTime(FriendChat chat) {
        return messageRepo.findTopByChatIdOrderByCreatedAtDesc(chat.getId())
            .map(Message::getCreatedAt)
            .orElse(chat.getCreatedAt());
    }

    // Get user id by email, throw exception if user not found
    private UUID requireUserIdByEmail(String email) {
        User user = userRepo.findByEmail(email);
        if (user == null) {
            throw new UserNotFoundException("User with email " + email + " not found");
        }
        return user.getId();
    }

    private String requireUserEmailById(UUID userId) {
        User user = userRepo.findById(userId).orElse(null);
        if (user == null) {
            throw new UserNotFoundException("User with id " + userId + " not found");
        }
        return user.getEmail();
    }

    // get the other user's id in the chat, given one user's id
    private UUID resolveCounterparty(FriendChat chat, UUID senderId) {
        if (chat.getUser1Id().equals(senderId)) {
            return chat.getUser2Id();
        }
        if (chat.getUser2Id().equals(senderId)) {
            return chat.getUser1Id();
        }
        throw new ChatMembershipException("Sender does not belong to the chat");
    }

    // Check if two users are accepted friends by verifying that they have a friend chat and an accepted friend request between them
    private boolean areAcceptedFriends(UUID userA, UUID userB) {
        boolean hasChat = chatRepo.findByUsers(userA, userB).isPresent();
        boolean hasAcceptedRequest = requestRepo.existsBySenderIdAndReceiverIdAndStatus(userA, userB, FriendRequestStatus.ACCEPTED)
            || requestRepo.existsBySenderIdAndReceiverIdAndStatus(userB, userA, FriendRequestStatus.ACCEPTED);

        return hasChat && hasAcceptedRequest;
    }

    // Check if either user has blocked the other
    private boolean isBlocked(UUID userA, UUID userB) {
        return blockedUserRepo.existsByUserIdAndBlockedUserId(userA, userB)
            || blockedUserRepo.existsByUserIdAndBlockedUserId(userB, userA);
    }

    // validate input for sending message, throw exception if invalid
    private void validateSendMessageInput(String chatId, String content, String nonce, String clientMessageId, String tag) {
        try {
            UUID.fromString(chatId);
        } catch (RuntimeException ex) {
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

        if (nonce == null || nonce.isBlank()) {
            throw new MessageValidationException("Nonce cannot be empty");
        }

        if (nonce.length() > MAX_NONCE_LENGTH) {
            throw new MessageValidationException("Nonce exceeds size limit");
        }

        if (clientMessageId == null || clientMessageId.isBlank()) {
            throw new MessageValidationException("clientMessageId cannot be empty");
        }

        if (tag == null || tag.isBlank()) {
            throw new MessageValidationException("Tag cannot be empty");
        }
    }

    // Convert a message entity into a response DTO
    private MessageDto toMessageDto(Message message) {
        return MessageDto.builder()
            .chatId(message.getChatId())
            .senderId(message.getSenderId())
            .content(message.getContent())
            .nonce(message.getNonce())
            .clientMessageId(message.getClientMessageId())
            .tag(message.getTag())
            .sentAt(message.getCreatedAt())
            .status(message.getStatus())
            .build();
    }

    // Build a new outgoing message entity
    private Message createMessage(UUID chatId, UUID senderId, UUID receiverId, String content, String nonce, String clientMessageId, String tag, int ttlMinutes) {
        LocalDateTime expiresAt = ttlMinutes > 0
            ? LocalDateTime.now().plusMinutes(ttlMinutes)
            : null;

        return Message.builder()
            .chatId(chatId)
            .senderId(senderId)
            .receiverId(receiverId)
            .content(content)
            .nonce(nonce)
            .clientMessageId(clientMessageId)
            .tag(tag)
            .status(MessageStatus.SENT)
            .createdAt(LocalDateTime.now())
            .expiresAt(expiresAt)
            .build();
    }

    
}
