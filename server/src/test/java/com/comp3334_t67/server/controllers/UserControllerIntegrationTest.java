package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.TestKeyFactory;
import com.comp3334_t67.server.dtos.UploadPublicKeyRequest;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.services.RateLimitService;
import com.comp3334_t67.server.services.UserService;

@SpringBootTest
class UserControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private UserController controller;

    @MockitoSpyBean
    private UserService userService;

    @MockitoSpyBean
    private RateLimitService rateLimitService;

    @Test
    void profileEndpoints_shouldReturnUserAndKeyData() throws Exception {
        // Arrange: create user, session, and uploaded key.
        User user = userRepo.save(User.builder().email("PROFILE@EXAMPLE.COM").password("x".getBytes()).build());
        String pem = TestKeyFactory.generatePemPublicKey();
        userService.uploadPublicKey(user.getEmail(), pem);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", user.getEmail());

        // Act: call profile and key endpoints.
        var profile = controller.getProfile(session);
        var key = controller.getPublicKey(session);

        // Assert: endpoint payloads match stored data.
        assertEquals(user.getEmail(), profile.getBody().getData().getEmail());
        assertEquals(pem, key.getBody().getData().getPublicKey());
    }

    @Test
    void publicUserEndpoints_shouldReturnOtherUsersData() throws Exception {
        // Arrange: create another user and upload a key.
        User user = userRepo.save(User.builder().email("LOOKUP@EXAMPLE.COM").password("x".getBytes()).build());
        String pem = TestKeyFactory.generatePemPublicKey();
        userService.uploadPublicKey(user.getEmail(), pem);

        // Act: query by id.
        var userInfo = controller.getUserInfo(user.getId().toString());
        var publicKey = controller.getPublicKey(user.getId().toString());

        // Assert: both endpoints return the stored record.
        assertEquals(user.getEmail(), userInfo.getBody().getData().getEmail());
        assertEquals(pem, publicKey.getBody().getData().getPublicKey());
    }

    @Test
    void uploadPublicKey_shouldRateLimit_andDelegateToService() throws Exception {
        // Arrange: create user session and request payload.
        User user = userRepo.save(User.builder().email("UPK@EXAMPLE.COM").password("x".getBytes()).build());
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", user.getEmail());

        UploadPublicKeyRequest req = new UploadPublicKeyRequest();
        req.setPublicKey(TestKeyFactory.generatePemPublicKey());

        // Act: call upload endpoint.
        var response = controller.uploadPublicKey(req, session);

        // Assert: response success and interactions are verified.
        assertTrue(response.getBody().isSuccess());
        verify(rateLimitService).assertAllowed(org.mockito.ArgumentMatchers.contains("upload-public-key:user:"), org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.any());
        verify(userService).uploadPublicKey(org.mockito.ArgumentMatchers.eq(user.getEmail()), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void blockAndUnblock_shouldSucceed_forValidSession() {
        // Arrange: create blocker and target users with session.
        User blocker = userRepo.save(User.builder().email("BL@EXAMPLE.COM").password("x".getBytes()).build());
        User target = userRepo.save(User.builder().email("TG@EXAMPLE.COM").password("x".getBytes()).build());

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("OTP_USER", blocker.getEmail());

        // Act: block then unblock target user.
        var blockResponse = controller.blockUser(target.getId().toString(), session);
        var unblockResponse = controller.unblockUser(target.getId().toString(), session);

        // Assert: both endpoints succeed.
        assertTrue(blockResponse.getBody().isSuccess());
        assertTrue(unblockResponse.getBody().isSuccess());
    }

    @Test
    void endpoints_shouldThrow_whenSessionMissingUser() {
        // Arrange: create empty session.
        MockHttpSession session = new MockHttpSession();

        // Act + Assert: protected endpoint rejects missing authentication.
        assertThrows(IllegalStateException.class, () -> controller.getProfile(session));
    }
}
