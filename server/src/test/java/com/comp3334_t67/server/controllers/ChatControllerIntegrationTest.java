package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.dtos.SendMessageRequest;
import com.comp3334_t67.server.enums.FriendRequestStatus;
import com.comp3334_t67.server.enums.MessageStatus;
import com.comp3334_t67.server.models.FriendChat;
import com.comp3334_t67.server.models.FriendRequest;
import com.comp3334_t67.server.models.Message;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.services.ChatService;

@SpringBootTest
class ChatControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ChatController controller;

    @MockitoSpyBean
    private ChatService chatService;

    @Test
    void sendMessage_shouldSucceed_andVerifyServiceCall() {
        // Arrange: create valid friendship graph and session user.
        User sender = userRepo.save(User.builder().email("CHAT1@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("CHAT2@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(sender.getId()).user2Id(receiver.getId()).createdAt(LocalDateTime.now()).build());
        friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.ACCEPTED).createdAt(LocalDateTime.now()).build());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", sender.getEmail());

        SendMessageRequest req = new SendMessageRequest();
        req.setContent("abc123==");
        req.setNonce("nonce");

        // Act: call endpoint.
        var response = controller.sendMessage(chat.getId().toString(), req, session);

        // Assert: endpoint succeeds and delegates to service.
        assertTrue(response.getBody().isSuccess());
        verify(chatService).sendMessage(chat.getId().toString(), sender.getEmail(), "abc123==", "nonce");
    }

    @Test
    void getMessages_shouldReturnUnreadMessages() {
        // Arrange: create chat with one unread message and receiver session.
        User sender = userRepo.save(User.builder().email("CHATS@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("CHATR@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(sender.getId()).user2Id(receiver.getId()).createdAt(LocalDateTime.now()).build());
        messageRepo.save(Message.builder().chatId(chat.getId()).senderId(sender.getId()).receiverId(receiver.getId()).content("enc").nonce("n").status(MessageStatus.SENT).createdAt(LocalDateTime.now()).build());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", receiver.getEmail());

        // Act: load unread messages via controller.
        var response = controller.getMessages(chat.getId().toString(), session);

        // Assert: one message is returned and service interaction is verified.
        assertTrue(response.getBody().isSuccess());
        assertEquals(1, response.getBody().getData().size());
        verify(chatService).getUnreadMessagesForReceiver(receiver.getEmail(), chat.getId().toString());
    }

    @Test
    void getFriendChats_shouldThrow_whenSessionUserMissing() {
        // Arrange: session with no OTP user.
        MockHttpSession session = new MockHttpSession();

        // Act + Assert: endpoint rejects unauthenticated session.
        assertThrows(IllegalStateException.class, () -> controller.getFriendChats(session));
    }
}
