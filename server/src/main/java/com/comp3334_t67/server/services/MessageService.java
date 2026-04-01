package com.comp3334_t67.server.services;

import org.springframework.stereotype.Service;

import com.comp3334_t67.server.enums.MessageStatus;
import com.comp3334_t67.server.models.*;
import com.comp3334_t67.server.repos.*;

import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.*;

@Service
@AllArgsConstructor
public class MessageService {

    private final int MESSAGE_EXPIRATION_MINUTES = 10;
    
    private final MessageRepository messageRepo;
    private final FriendChatRepository chatRepo;
    private final UserRepository userRepo;

    // send message
    public void sendMessage(String chatId, String senderEmail, String content, String nouce) {
        
        // retrieve sender and receiver id
        UUID senderId = userRepo.findByEmail(senderEmail).getId();
        
        FriendChat chat = chatRepo.findById(UUID.fromString(chatId)).get();
        UUID receiverId = chat.getUser1Id().equals(senderId) ? chat.getUser2Id() : chat.getUser1Id();

        // create message
        Message message = createMessage(senderId, receiverId, content, nouce);

        // save message to database
        messageRepo.save(message);    
    }

    // get unread messages for a user
    public List<Message> getUnreadMessagesForReceiver(String receiverEmail, String chatId) {
        UUID receiverId = userRepo.findByEmail(receiverEmail).getId();
        List<Message> unreadMessages = messageRepo.findByReceiverIdAndChatIdAndStatus(receiverId, UUID.fromString(chatId), MessageStatus.SENT);

        // update message status to DELIVERED and set delivered_at timestamp
        for (Message message : unreadMessages) {
            message.setStatus(MessageStatus.DELIVERED);
            message.setDelivered_at(LocalDateTime.now());
            messageRepo.save(message);
        }

        return unreadMessages;
    }

    // Create new message
    private Message createMessage(UUID senderId, UUID receiverId, String content, String nouce) {
        Message message = Message.builder()
            .senderId(senderId)
            .receiverId(receiverId)
            .content_hashed(content)
            .nouce(nouce)
            .status(MessageStatus.SENT)
            .created_at(LocalDateTime.now())
            .expires_at(LocalDateTime.now().plusMinutes(MESSAGE_EXPIRATION_MINUTES))
            .build();
        
        return message;
    }

}
