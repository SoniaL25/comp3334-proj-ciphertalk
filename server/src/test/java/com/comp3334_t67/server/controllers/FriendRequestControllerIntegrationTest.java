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
import com.comp3334_t67.server.Exceptions.OtpSessionMissingException;
import com.comp3334_t67.server.dtos.ActionRequest;
import com.comp3334_t67.server.dtos.FriendReqRequest;
import com.comp3334_t67.server.enums.FriendRequestStatus;
import com.comp3334_t67.server.models.FriendRequest;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.services.FriendRequestService;
import com.comp3334_t67.server.services.RateLimitService;

@SpringBootTest
class FriendRequestControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private FriendRequestController controller;

    @MockitoSpyBean
    private FriendRequestService friendService;

    @MockitoSpyBean
    private RateLimitService rateLimitService;

    @Test
    void sendFriendRequest_shouldSucceed_andVerifyInteraction() {
        // Arrange: create sender/receiver and session.
        User sender = userRepo.save(User.builder().email("FRS@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("FRR@EXAMPLE.COM").password("x".getBytes()).build());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", sender.getEmail());

        FriendReqRequest request = new FriendReqRequest();
        request.setReceiverEmail(receiver.getEmail());

        // Act: send request.
        var response = controller.sendFriendRequest(request, session);

        // Assert: success response and service calls are verified.
        assertTrue(response.getBody().isSuccess());
        verify(rateLimitService).assertAllowed(org.mockito.ArgumentMatchers.contains("friend-request:user:"), org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.any());
        verify(friendService).sendFriendRequest(sender.getEmail(), receiver.getEmail());
    }

    @Test
    void respondToFriendRequest_shouldSucceed() {
        // Arrange: create pending request and receiver session.
        User sender = userRepo.save(User.builder().email("R1@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("R2@EXAMPLE.COM").password("x".getBytes()).build());
        FriendRequest request = friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.PENDING).createdAt(LocalDateTime.now()).build());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", receiver.getEmail());

        ActionRequest action = new ActionRequest();
        action.setAction(FriendRequestStatus.ACCEPTED);

        // Act: accept request.
        var response = controller.respondToFriendRequest(request.getId().toString(), action, session);

        // Assert: response is successful and backend service was called.
        assertTrue(response.getBody().isSuccess());
        verify(friendService).respondToFriendRequest(receiver.getEmail(), request.getId().toString(), true);
    }

    @Test
    void cancelAndListEndpoints_shouldHandleSessionValidation() {
        // Arrange: create session without OTP_USER.
        MockHttpSession emptySession = new MockHttpSession();

        // Act + Assert: cancel requires active OTP session.
        assertThrows(OtpSessionMissingException.class, () -> controller.cancelFriendRequest("x", emptySession));

        // Act + Assert: incoming/outgoing also require OTP session.
        assertThrows(OtpSessionMissingException.class, () -> controller.getAllIncomingRequests(emptySession));
        assertThrows(OtpSessionMissingException.class, () -> controller.getAllOutgoingRequests(emptySession));
    }

    @Test
    void incomingAndOutgoing_shouldReturnLists() {
        // Arrange: create users, one pending request, and session.
        User sender = userRepo.save(User.builder().email("OUT@EXAMPLE.COM").password("x".getBytes()).build());
        User receiver = userRepo.save(User.builder().email("IN@EXAMPLE.COM").password("x".getBytes()).build());
        friendRequestRepo.save(FriendRequest.builder().senderId(sender.getId()).receiverId(receiver.getId()).status(FriendRequestStatus.PENDING).createdAt(LocalDateTime.now()).build());

        MockHttpSession senderSession = new MockHttpSession();
        senderSession.setAttribute("OTP_USER", sender.getEmail());
        MockHttpSession receiverSession = new MockHttpSession();
        receiverSession.setAttribute("OTP_USER", receiver.getEmail());

        // Act: fetch incoming/outgoing requests.
        var incoming = controller.getAllIncomingRequests(receiverSession);
        var outgoing = controller.getAllOutgoingRequests(senderSession);

        // Assert: both lists include the pending request.
        assertEquals(1, incoming.getBody().getData().size());
        assertEquals(1, outgoing.getBody().getData().size());
    }
}
