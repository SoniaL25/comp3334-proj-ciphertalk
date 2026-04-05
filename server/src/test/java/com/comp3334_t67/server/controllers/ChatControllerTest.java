package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.dtos.FriendChatDto;
import com.comp3334_t67.server.dtos.MessageDto;
import com.comp3334_t67.server.dtos.SendMessageRequest;
import com.comp3334_t67.server.services.ChatService;
import com.comp3334_t67.server.services.MessageService;

import org.springframework.mock.web.MockHttpSession;

class ChatControllerTest {

    private MessageService messageService;
    private ChatService chatService;
    private ChatController controller;

    @BeforeEach
    void setUp() {
        messageService = mock(MessageService.class);
        chatService = mock(ChatService.class);
        controller = new ChatController(messageService, chatService);
    }

    @Test
    void getFriendChats_shouldReturnChats() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "u@x.com");
        when(chatService.getFriendChats("u@x.com")).thenReturn(List.of(FriendChatDto.builder().build()));

        var response = controller.getFriendChats(session);

        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
    }

    @Test
    void sendMessage_shouldDelegateToService() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "u@x.com");
        String chatId = UUID.randomUUID().toString();
        SendMessageRequest req = new SendMessageRequest();
        req.setContent("abc123==");
        req.setNonce("nonce");

        var response = controller.sendMessage(chatId, req, session);

        assertTrue(response.getBody().isSuccess());
        verify(chatService).sendMessage(chatId, "u@x.com", "abc123==", "nonce");
    }

    @Test
    void getMessages_shouldReturnUnreadMessages() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "u@x.com");
        String chatId = UUID.randomUUID().toString();
        when(messageService.getUnreadMessagesForReceiver("u@x.com", chatId)).thenReturn(List.of(MessageDto.builder().content("m1").build()));

        var response = controller.getMessages(chatId, session);

        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("m1", response.getBody().getData().get(0).getContent());
    }

    @Test
    void removeFriend_shouldThrowWhenSessionMissingUser() {
        MockHttpSession session = new MockHttpSession();
        String chatId = UUID.randomUUID().toString();

        assertThrows(IllegalStateException.class, () -> controller.removeFriend(chatId, session));
    }
}
