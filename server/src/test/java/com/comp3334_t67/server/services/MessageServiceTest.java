package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.enums.MessageStatus;
import com.comp3334_t67.server.models.FriendChat;
import com.comp3334_t67.server.models.Message;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.FriendChatRepository;
import com.comp3334_t67.server.repos.MessageRepository;
import com.comp3334_t67.server.repos.UserRepository;

class MessageServiceTest {

    private MessageRepository messageRepo;
    private FriendChatRepository chatRepo;
    private UserRepository userRepo;
    private MessageService service;

    @BeforeEach
    void setUp() {
        messageRepo = mock(MessageRepository.class);
        chatRepo = mock(FriendChatRepository.class);
        userRepo = mock(UserRepository.class);
        service = new MessageService(messageRepo, chatRepo, userRepo);
    }

    @Test
    void sendMessage_shouldSaveNewMessage() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        when(userRepo.findByEmail("a@x.com")).thenReturn(User.builder().id(sender).build());
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(FriendChat.builder().id(chatId).user1Id(sender).user2Id(receiver).build()));

        service.sendMessage(chatId.toString(), "a@x.com", "hash", "nonce");

        verify(messageRepo).save(any(Message.class));
    }

    @Test
    void getUnreadMessagesForReceiver_shouldMarkAsDelivered() {
        UUID receiver = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        when(userRepo.findByEmail("r@x.com")).thenReturn(User.builder().id(receiver).build());
        Message m = Message.builder().status(MessageStatus.SENT).build();
        when(messageRepo.findByReceiverIdAndChatIdAndStatus(receiver, chatId, MessageStatus.SENT)).thenReturn(List.of(m));

        List<Message> result = service.getUnreadMessagesForReceiver("r@x.com", chatId.toString());

        assertEquals(1, result.size());
        assertEquals(MessageStatus.DELIVERED, m.getStatus());
        verify(messageRepo).save(m);
    }
}
