package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.Exceptions.OtpSessionMissingException;
import com.comp3334_t67.server.dtos.ActionRequest;
import com.comp3334_t67.server.dtos.FriendReqRequest;
import com.comp3334_t67.server.dtos.FriendRequestDto;
import com.comp3334_t67.server.services.FriendRequestService;
import com.comp3334_t67.server.services.RateLimitService;

import org.springframework.mock.web.MockHttpSession;

class FriendRequestControllerTest {

    private FriendRequestService friendService;
    private RateLimitService rateLimitService;
    private FriendRequestController controller;

    @BeforeEach
    void setUp() {
        friendService = mock(FriendRequestService.class);
        rateLimitService = mock(RateLimitService.class);
        controller = new FriendRequestController(friendService, rateLimitService);
    }

    @Test
    void sendFriendRequest_shouldApplyRateLimitAndDelegate() {
        FriendReqRequest req = new FriendReqRequest();
        req.setSenderEmail("a@x.com");
        req.setReceiverEmail("b@x.com");

        controller.sendFriendRequest(req);

        verify(rateLimitService).assertAllowed(startsWith("friend-request:"), eq(10), any());
        verify(friendService).sendFriendRequest("a@x.com", "b@x.com");
    }

    @Test
    void respondToFriendRequest_shouldDelegate() {
        ActionRequest req = new ActionRequest();
        req.setAccepted(true);

        controller.respondToFriendRequest("r1", req);

        verify(friendService).respondToFriendRequest("r1", true);
    }

    @Test
    void cancel_shouldThrowWithoutSessionEmail() {
        MockHttpSession session = new MockHttpSession();

        assertThrows(OtpSessionMissingException.class, () -> controller.cancelFriendRequest("id", session));
    }

    @Test
    void incoming_shouldReturnDtos() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("email", "receiver@x.com");
        when(friendService.getIncomingFriendRequests("receiver@x.com")).thenReturn(List.of(new FriendRequestDto()));

        List<FriendRequestDto> result = controller.getAllIncomingRequests(session);

        assertEquals(1, result.size());
    }
}
