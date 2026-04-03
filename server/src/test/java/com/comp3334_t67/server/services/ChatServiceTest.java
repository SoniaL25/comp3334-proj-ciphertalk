package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.Exceptions.ChatMembershipException;
import com.comp3334_t67.server.Exceptions.MessageValidationException;
import com.comp3334_t67.server.Exceptions.MessagingBlockedException;
import com.comp3334_t67.server.dtos.FriendChatDto;
import com.comp3334_t67.server.enums.FriendRequestStatus;
import com.comp3334_t67.server.enums.MessageStatus;
import com.comp3334_t67.server.models.BlockedUser;
import com.comp3334_t67.server.models.FriendChat;
import com.comp3334_t67.server.models.Message;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.BlockedUserRepository;
import com.comp3334_t67.server.repos.FriendChatRepository;
import com.comp3334_t67.server.repos.FriendRequestRepository;
import com.comp3334_t67.server.repos.MessageRepository;
import com.comp3334_t67.server.repos.UserRepository;

class ChatServiceTest {

    private FriendChatRepository chatRepo;
    private FriendRequestRepository requestRepo;
    private MessageRepository messageRepo;
    private BlockedUserRepository blockedUserRepo;
    private UserRepository userRepo;
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatRepo = mock(FriendChatRepository.class);
        requestRepo = mock(FriendRequestRepository.class);
        messageRepo = mock(MessageRepository.class);
        blockedUserRepo = mock(BlockedUserRepository.class);
        userRepo = mock(UserRepository.class);
        chatService = new ChatService(chatRepo, requestRepo, messageRepo, blockedUserRepo, userRepo);
    }

    @Test
    void sendMessage_shouldThrowForInvalidChatId() {
        assertThrows(MessageValidationException.class,
            () -> chatService.sendMessage("not-uuid", "a@x.com", "abc", "nonce"));
    }

    @Test
    void sendMessage_shouldThrowWhenBlocked() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        when(userRepo.findByEmail("a@x.com")).thenReturn(User.builder().id(sender).email("a@x.com").build());
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(FriendChat.builder().id(chatId).user1Id(sender).user2Id(receiver).build()));
        when(chatRepo.findByUsers(sender, receiver)).thenReturn(Optional.of(FriendChat.builder().id(chatId).build()));
        when(requestRepo.existsBySenderIdAndReceiverIdAndStatus(any(), any(), eq(FriendRequestStatus.ACCEPTED))).thenReturn(true);
        when(blockedUserRepo.existsByUserIdAndBlockedUserId(any(), any())).thenReturn(true);

        assertThrows(MessagingBlockedException.class,
            () -> chatService.sendMessage(chatId.toString(), "a@x.com", "abc123==", "nonce"));
    }

    @Test
    void sendMessage_shouldSaveWhenValid() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        when(userRepo.findByEmail("a@x.com")).thenReturn(User.builder().id(sender).email("a@x.com").build());
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(FriendChat.builder().id(chatId).user1Id(sender).user2Id(receiver).build()));
        when(chatRepo.findByUsers(sender, receiver)).thenReturn(Optional.of(FriendChat.builder().id(chatId).build()));
        when(requestRepo.existsBySenderIdAndReceiverIdAndStatus(any(), any(), eq(FriendRequestStatus.ACCEPTED))).thenReturn(true);
        when(blockedUserRepo.existsByUserIdAndBlockedUserId(any(), any())).thenReturn(false);

        chatService.sendMessage(chatId.toString(), "a@x.com", "abc123==", "nonce");

        verify(messageRepo).save(any(Message.class));
    }

    @Test
    void removeFriend_shouldThrowWhenRequesterNotMember() {
        UUID requester = UUID.randomUUID();
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        UUID chatId = UUID.randomUUID();
        when(userRepo.findByEmail("req@x.com")).thenReturn(User.builder().id(requester).email("req@x.com").build());
        when(chatRepo.findById(chatId)).thenReturn(Optional.of(FriendChat.builder().id(chatId).user1Id(user1).user2Id(user2).build()));

        assertThrows(ChatMembershipException.class,
            () -> chatService.removeFriend(chatId.toString(), "req@x.com"));
    }

    @Test
    void getFriendChats_shouldFilterBlockedAndMarkDelivered() {
        UUID me = UUID.randomUUID();
        UUID friend = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        UUID chat1Id = UUID.randomUUID();
        UUID chat2Id = UUID.randomUUID();

        when(userRepo.findByEmail("me@x.com")).thenReturn(User.builder().id(me).email("me@x.com").build());
        when(blockedUserRepo.findByUserId(me)).thenReturn(List.of(BlockedUser.builder().blockedUserId(blocked).build()));

        FriendChat chat1 = FriendChat.builder().id(chat1Id).user1Id(me).user2Id(friend).createdAt(LocalDateTime.now().minusDays(1)).build();
        FriendChat chat2 = FriendChat.builder().id(chat2Id).user1Id(me).user2Id(blocked).createdAt(LocalDateTime.now().minusDays(2)).build();
        when(chatRepo.findAllByUserId(me)).thenReturn(List.of(chat1, chat2));

        Message unread = Message.builder().status(MessageStatus.SENT).build();
        when(messageRepo.findByReceiverIdAndChatIdAndStatus(me, chat1Id, MessageStatus.SENT)).thenReturn(List.of(unread));
        when(messageRepo.findTopByChatIdOrderByCreatedAtDesc(chat1Id))
            .thenReturn(Optional.of(Message.builder().createdAt(LocalDateTime.now()).build()));

        List<FriendChatDto> result = chatService.getFriendChats("me@x.com");

        assertEquals(1, result.size());
        assertEquals(friend, result.get(0).getSenderId());
        assertEquals(MessageStatus.DELIVERED, unread.getStatus());
        verify(messageRepo).saveAll(anyList());
    }
}
