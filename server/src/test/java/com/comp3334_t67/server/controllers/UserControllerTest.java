package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.dtos.KeyDto;
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
    void getProfile_shouldReturnUserDto() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "user@x.com");
        when(userService.getUserInfoByEmail("user@x.com")).thenReturn(UserDto.builder().email("user@x.com").build());

        var response = controller.getProfile(session);

        assertTrue(response.getBody().isSuccess());
        assertEquals("user@x.com", response.getBody().getData().getEmail());
    }

    @Test
    void uploadPublicKey_shouldDelegateWithSession() {
        UploadPublicKeyRequest req = new UploadPublicKeyRequest();
        req.setPublicKey("pk");
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "user@x.com");

        var response = controller.uploadPublicKey(req, session);

        assertTrue(response.getBody().isSuccess());
        verify(userService).uploadPublicKey("user@x.com", "pk");
    }

    @Test
    void getPublicKey_shouldReturnKeyDtoForCurrentUser() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "user@x.com");
        when(userService.getPublicKey("user@x.com")).thenReturn(KeyDto.builder().publicKey("pk").build());

        var response = controller.getPublicKey(session);

        assertTrue(response.getBody().isSuccess());
        assertEquals("pk", response.getBody().getData().getPublicKey());
    }

    @Test
    void getPublicKeyById_shouldReturnKeyDto() {
        when(userService.getPublicKey("u1")).thenReturn(KeyDto.builder().publicKey("pk").build());

        var response = controller.getPublicKey("u1");

        assertTrue(response.getBody().isSuccess());
        assertEquals("pk", response.getBody().getData().getPublicKey());
    }

    @Test
    void blockUser_shouldUseSessionEmail() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "blocker@x.com");

        var response = controller.blockUser("u1", session);

        assertTrue(response.getBody().isSuccess());
        verify(userService).blockUser("blocker@x.com", "u1");
    }

    @Test
    void unblockUser_shouldUseSessionEmailAndDeleteBlock() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", "blocker@x.com");

        var response = controller.unblockUser("u2", session);

        assertTrue(response.getBody().isSuccess());
        verify(userService).unblockUser("blocker@x.com", "u2");
    }

    @Test
    void unblockUser_shouldThrowWithoutSessionUser() {
        MockHttpSession session = new MockHttpSession();

        assertThrows(IllegalStateException.class, () -> controller.unblockUser("u1", session));
    }
}
