package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.Exceptions.FriendRequestOwnershipException;
import com.comp3334_t67.server.enums.FriendRequestStatus;
import com.comp3334_t67.server.models.FriendRequest;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.FriendChatRepository;
import com.comp3334_t67.server.repos.FriendRequestRepository;
import com.comp3334_t67.server.repos.UserRepository;

class FriendRequestServiceTest {

    private FriendRequestRepository requestRepo;
    private FriendChatRepository chatRepo;
    private UserRepository userRepo;
    private FriendRequestService service;

    @BeforeEach
    void setUp() {
        requestRepo = mock(FriendRequestRepository.class);
        chatRepo = mock(FriendChatRepository.class);
        userRepo = mock(UserRepository.class);
        service = new FriendRequestService(requestRepo, chatRepo, userRepo);
    }

    @Test
    void sendFriendRequest_shouldSavePendingRequest() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        when(userRepo.findByEmail("a@x.com")).thenReturn(User.builder().id(sender).email("a@x.com").build());
        when(userRepo.findByEmail("b@x.com")).thenReturn(User.builder().id(receiver).email("b@x.com").build());

        service.sendFriendRequest("a@x.com", "b@x.com");

        verify(requestRepo).save(any(FriendRequest.class));
    }

    @Test
    void respondToFriendRequest_accept_shouldCreateChatAndSave() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        FriendRequest request = FriendRequest.builder()
            .id(requestId)
            .senderId(sender)
            .receiverId(receiver)
            .status(FriendRequestStatus.PENDING)
            .build();
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(request));

        service.respondToFriendRequest(requestId.toString(), true);

        assertEquals(FriendRequestStatus.ACCEPTED, request.getStatus());
        verify(chatRepo, atLeastOnce()).save(any());
        verify(requestRepo).save(request);
    }

    @Test
    void cancelFriendRequest_shouldThrowWhenNotSender() {
        UUID sender = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        when(userRepo.findByEmail("a@x.com")).thenReturn(User.builder().id(sender).email("a@x.com").build());
        when(requestRepo.findById(requestId)).thenReturn(Optional.of(
            FriendRequest.builder().id(requestId).senderId(other).status(FriendRequestStatus.PENDING).build()
        ));

        assertThrows(FriendRequestOwnershipException.class,
            () -> service.cancelFriendRequest("a@x.com", requestId.toString()));
    }
}
