package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.Exceptions.FriendRequestOwnershipException;
import com.comp3334_t67.server.Exceptions.FriendRequestStateException;
import com.comp3334_t67.server.dtos.FriendRequestDto;
import com.comp3334_t67.server.enums.FriendRequestStatus;
import com.comp3334_t67.server.models.FriendRequest;
import com.comp3334_t67.server.models.User;

@SpringBootTest
class FriendRequestServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private FriendRequestService service;

    @Test
    void sendFriendRequest_shouldCreatePendingRequest() {
        // Arrange: create sender and receiver.
        User sender = userRepo.save(User.builder().email("S@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("R@EXAMPLE.COM").password("x".getBytes()).build());

        // Act: send friend request.
        assertDoesNotThrow(() -> service.sendFriendRequest(sender.getEmail(), receiver.getEmail()));

        // Assert: exactly one pending request is persisted.
        List<FriendRequest> requests = friendRequestRepo.findBySenderIdAndStatus(sender.getId(), FriendRequestStatus.PENDING);
        assertEquals(1, requests.size());
        assertEquals(receiver.getId(), requests.get(0).getReceiverId());
    }

    @Test
    void respondToFriendRequest_shouldAccept_andCreateChat() {
        // Arrange: seed sender, receiver, and pending request.
        User sender = userRepo.save(User.builder().email("A@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("B@EXAMPLE.COM").password("x".getBytes()).build());
        FriendRequest request = friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.PENDING).createdAt(LocalDateTime.now()).build());

        // Act: receiver accepts request.
        assertDoesNotThrow(() -> service.respondToFriendRequest(receiver.getEmail(), request.getId().toString(), true));

        // Assert: request is accepted and chat is created.
        FriendRequest updated = friendRequestRepo.findById(request.getId()).orElseThrow();
        assertEquals(FriendRequestStatus.ACCEPTED, updated.getStatus());
        assertEquals(1, friendChatRepo.findAll().size());
    }

    @Test
    void respondToFriendRequest_shouldReject_withoutCreatingChat() {
        // Arrange: create pending request.
        User sender = userRepo.save(User.builder().email("C@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("D@EXAMPLE.COM").password("x".getBytes()).build());
        FriendRequest request = friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.PENDING).createdAt(LocalDateTime.now()).build());

        // Act: reject request.
        assertDoesNotThrow(() -> service.respondToFriendRequest(receiver.getEmail(), request.getId().toString(), false));

        // Assert: request is rejected and no chat exists.
        FriendRequest updated = friendRequestRepo.findById(request.getId()).orElseThrow();
        assertEquals(FriendRequestStatus.REJECTED, updated.getStatus());
        assertTrue(friendChatRepo.findAll().isEmpty());
    }

    @Test
    void respondToFriendRequest_shouldThrow_whenWrongReceiverResponds() {
        // Arrange: create pending request with one receiver.
        User sender = userRepo.save(User.builder().email("E@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("F@EXAMPLE.COM").password("x".getBytes()).build());
        User stranger = userRepo.save(User.builder().email("G@EXAMPLE.COM").password("x".getBytes()).build());
        FriendRequest request = friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.PENDING).createdAt(LocalDateTime.now()).build());

        // Act + Assert: non-receiver is rejected.
        assertThrows(FriendRequestOwnershipException.class, () -> service.respondToFriendRequest(stranger.getEmail(), request.getId().toString(), true));
    }

    @Test
    void cancelFriendRequest_shouldCancel_whenSenderOwnsRequest() {
        // Arrange: create pending request.
        User sender = userRepo.save(User.builder().email("H@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("I@EXAMPLE.COM").password("x".getBytes()).build());
        FriendRequest request = friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.PENDING).createdAt(LocalDateTime.now()).build());

        // Act: sender cancels request.
        assertDoesNotThrow(() -> service.cancelFriendRequest(sender.getEmail(), request.getId().toString()));

        // Assert: status becomes CANCELED.
        assertEquals(FriendRequestStatus.CANCELED, friendRequestRepo.findById(request.getId()).orElseThrow().getStatus());
    }

    @Test
    void cancelFriendRequest_shouldThrow_whenRequestAlreadyHandled() {
        // Arrange: create accepted request.
        User sender = userRepo.save(User.builder().email("J@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("K@EXAMPLE.COM").password("x".getBytes()).build());
        FriendRequest request = friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.ACCEPTED).createdAt(LocalDateTime.now()).build());

        // Act + Assert: cannot cancel non-pending request.
        assertThrows(FriendRequestStateException.class, () -> service.cancelFriendRequest(sender.getEmail(), request.getId().toString()));
    }

    @Test
    void getIncomingAndOutgoing_shouldReturnDtosForPendingOnly() {
        // Arrange: create pending and rejected requests around same users.
        User sender = userRepo.save(User.builder().email("L@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("M@EXAMPLE.COM").password("x".getBytes()).build());
        friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.PENDING).createdAt(LocalDateTime.now()).build());
        friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.REJECTED).createdAt(LocalDateTime.now()).build());

        // Act: load incoming and outgoing lists.
        List<FriendRequestDto> incoming = service.getIncomingFriendRequests(receiver.getEmail());
        List<FriendRequestDto> outgoing = service.getOutgoingFriendRequests(sender.getEmail());

        // Assert: only pending entries are included.
        assertEquals(1, incoming.size());
        assertEquals(1, outgoing.size());
        assertEquals(FriendRequestStatus.PENDING, incoming.get(0).getStatus());
        assertEquals(FriendRequestStatus.PENDING, outgoing.get(0).getStatus());
    }
}
