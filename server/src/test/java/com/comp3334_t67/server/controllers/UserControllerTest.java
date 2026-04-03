package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.dtos.UploadPublicKeyRequest;
import com.comp3334_t67.server.dtos.UserDto;
import com.comp3334_t67.server.services.UserService;

import org.springframework.mock.web.MockHttpSession;

class UserControllerTest {

    private UserService userService;
    private UserController controller;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        controller = new UserController(userService);
    }

    @Test
    void getUserInfo_shouldReturnDto() {
        when(userService.getUserInfoById("u1")).thenReturn(UserDto.builder().email("a@x.com").build());

        var response = controller.getUserInfo("u1");

        assertTrue(response.getBody().isSuccess());
        assertEquals("a@x.com", response.getBody().getData().getEmail());
    }

    @Test
    void uploadPublicKey_shouldDelegate() {
        UploadPublicKeyRequest req = new UploadPublicKeyRequest();
        req.setPublicKey("pk");

        var response = controller.uploadPublicKey("u1", req);

        assertTrue(response.getBody().isSuccess());
        verify(userService).uploadPublicKey("u1", "pk");
    }

    @Test
    void blockUser_shouldUseSessionEmail() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "blocker@x.com");

        var response = controller.blockUser("blocked@x.com", session);

        assertTrue(response.getBody().isSuccess());
        verify(userService).blockUser("blocker@x.com", "blocked@x.com");
    }

    @Test
    void unblockUser_shouldThrowWithoutSessionUser() {
        MockHttpSession session = new MockHttpSession();

        assertThrows(IllegalStateException.class, () -> controller.unblockUser("blocked@x.com", session));
    }
}
