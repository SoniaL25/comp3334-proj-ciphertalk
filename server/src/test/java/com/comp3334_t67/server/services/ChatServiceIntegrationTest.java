package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.Exceptions.ChatMembershipException;
import com.comp3334_t67.server.Exceptions.MessageValidationException;
import com.comp3334_t67.server.Exceptions.MessagingBlockedException;
import com.comp3334_t67.server.Exceptions.UsersNotFriendsException;
import com.comp3334_t67.server.dtos.FriendChatDto;
import com.comp3334_t67.server.dtos.MessageDto;
import com.comp3334_t67.server.enums.FriendRequestStatus;
import com.comp3334_t67.server.enums.MessageStatus;
import com.comp3334_t67.server.models.BlockedUser;
import com.comp3334_t67.server.models.FriendChat;
import com.comp3334_t67.server.models.FriendRequest;
import com.comp3334_t67.server.models.Message;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.MessageRepository;

@SpringBootTest
class ChatServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ChatService chatService;

    @MockitoSpyBean
    private MessageRepository messageRepositorySpy;

    @Test
    void sendMessage_shouldPersist_whenChatAndFriendshipAreValid() {
        // Arrange: create sender, receiver, chat, and accepted request.
        User sender = userRepo.save(User.builder().email("SENDER@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("RECV@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(sender.getId()).user2Id(receiver.getId()).createdAt(LocalDateTime.now()).build());
        friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.ACCEPTED).createdAt(LocalDateTime.now()).build());

        // Act: send one message.
        assertDoesNotThrow(() -> chatService.sendMessage(chat.getId().toString(), sender.getEmail(), "abc123==", "nonce123"));

        // Assert: one message was persisted for the chat.
        List<Message> stored = messageRepo.findAll();
        assertEquals(1, stored.size());
        assertEquals(chat.getId(), stored.get(0).getChatId());
    }

    @Test
    void sendMessage_shouldThrow_whenChatIdIsInvalid() {
        // Arrange + Act + Assert: malformed UUID should fail fast.
        assertThrows(MessageValidationException.class, () -> chatService.sendMessage("bad-id", "A@EXAMPLE.COM", "abc", "nonce"));
    }

    @Test
    void sendMessage_shouldThrow_whenUsersAreNotAcceptedFriends() {
        // Arrange: create users and chat but no accepted request.
        User sender = userRepo.save(User.builder().email("A@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("B@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(sender.getId()).user2Id(receiver.getId()).createdAt(LocalDateTime.now()).build());

        // Act + Assert: friendship check blocks sending.
        assertThrows(UsersNotFriendsException.class, () -> chatService.sendMessage(chat.getId().toString(), sender.getEmail(), "abc123==", "nonce"));
    }

    @Test
    void sendMessage_shouldThrow_whenEitherSideIsBlocked() {
        // Arrange: create valid friend relation then add block rule.
        User sender = userRepo.save(User.builder().email("BLOCKER@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("TARGET@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(sender.getId()).user2Id(receiver.getId()).createdAt(LocalDateTime.now()).build());
        friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.ACCEPTED).createdAt(LocalDateTime.now()).build());
        blockedUserRepo.save(BlockedUser.builder().userId(sender.getId()).blockedUserId(receiver.getId()).blockedAt(LocalDateTime.now()).build());

        // Act + Assert: blocked users cannot message.
        assertThrows(MessagingBlockedException.class, () -> chatService.sendMessage(chat.getId().toString(), sender.getEmail(), "abc123==", "nonce"));
    }

    @Test
    void getUnreadMessagesForReceiver_shouldMarkDelivered_andReturnDtos() {
        // Arrange: create users/chat and one unread message.
        User sender = userRepo.save(User.builder().email("MS@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("MR@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(sender.getId()).user2Id(receiver.getId()).createdAt(LocalDateTime.now()).build());
        messageRepo.save(Message.builder().chatId(chat.getId()).senderId(sender.getId()).receiverId(receiver.getId()).content("enc").nonce("n").status(MessageStatus.SENT).createdAt(LocalDateTime.now()).build());

        // Act: load unread messages for receiver.
        List<MessageDto> result = chatService.getUnreadMessagesForReceiver(receiver.getEmail(), chat.getId().toString());

        // Assert: dto returned and message status persisted as DELIVERED.
        assertEquals(1, result.size());
        assertEquals(MessageStatus.DELIVERED, result.get(0).getStatus());
        assertEquals(MessageStatus.DELIVERED, messageRepo.findAll().get(0).getStatus());
        verify(messageRepositorySpy).save(org.mockito.ArgumentMatchers.any(Message.class));
    }

    @Test
    void getFriendChats_shouldSkipBlockedChats_andSortNewestFirst() {
        // Arrange: create current user with one normal chat and one blocked chat.
        User me = userRepo.save(User.builder().email("ME@EXAMPLE.COM").password("x".getBytes()).build());
        User friend = userRepo.save(User.builder().email("F@EXAMPLE.COM").password("x".getBytes()).build());
        User blocked = userRepo.save(User.builder().email("B@EXAMPLE.COM").password("x".getBytes()).build());

        FriendChat visibleChat = friendChatRepo.save(FriendChat.builder().user1Id(me.getId()).user2Id(friend.getId()).createdAt(LocalDateTime.now().minusDays(1)).build());
        friendChatRepo.save(FriendChat.builder().user1Id(me.getId()).user2Id(blocked.getId()).createdAt(LocalDateTime.now().minusDays(2)).build());
        blockedUserRepo.save(BlockedUser.builder().userId(me.getId()).blockedUserId(blocked.getId()).blockedAt(LocalDateTime.now()).build());

        messageRepo.save(Message.builder().chatId(visibleChat.getId()).senderId(friend.getId()).receiverId(me.getId()).content("x").nonce("n").status(MessageStatus.SENT).createdAt(LocalDateTime.now()).build());

        // Act: fetch friend-chat summaries.
        List<FriendChatDto> chats = chatService.getFriendChats(me.getEmail());

        // Assert: blocked chat is filtered and unread is marked delivered.
        assertEquals(1, chats.size());
        assertEquals(friend.getId(), chats.get(0).getSenderId());
        assertEquals(1, chats.get(0).getNumOfUnreadMessage());
        assertEquals(MessageStatus.DELIVERED, messageRepo.findAll().get(0).getStatus());
    }

    @Test
    void removeFriend_shouldDelete_whenRequesterBelongsToChat() {
        // Arrange: create chat where requester is member.
        User requester = userRepo.save(User.builder().email("REQ@EXAMPLE.COM").password("x".getBytes()).build());
        User other = userRepo.save(User.builder().email("OTH@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(requester.getId()).user2Id(other.getId()).createdAt(LocalDateTime.now()).build());

        // Act: remove friend relation.
        assertDoesNotThrow(() -> chatService.removeFriend(chat.getId().toString(), requester.getEmail()));

        // Assert: chat is deleted.
        assertTrue(friendChatRepo.findById(chat.getId()).isEmpty());
    }

    @Test
    void removeFriend_shouldThrow_whenRequesterNotInChat() {
        // Arrange: create chat and unrelated requester.
        User user1 = userRepo.save(User.builder().email("U1@EXAMPLE.COM").password("x".getBytes()).build());
        User user2 = userRepo.save(User.builder().email("U2@EXAMPLE.COM").password("x".getBytes()).build());
        User outsider = userRepo.save(User.builder().email("OUT@EXAMPLE.COM").password("x".getBytes()).build());
        FriendChat chat = friendChatRepo.save(FriendChat.builder().user1Id(user1.getId()).user2Id(user2.getId()).createdAt(LocalDateTime.now()).build());

        // Act + Assert: outsider cannot remove the chat.
        assertThrows(ChatMembershipException.class, () -> chatService.removeFriend(chat.getId().toString(), outsider.getEmail()));
        assertFalse(friendChatRepo.findById(chat.getId()).isEmpty());
    }
}
