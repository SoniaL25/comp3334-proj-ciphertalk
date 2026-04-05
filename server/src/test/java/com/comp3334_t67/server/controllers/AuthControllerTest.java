package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.Exceptions.OtpInvalidException;
import com.comp3334_t67.server.dtos.AuthRequest;
import com.comp3334_t67.server.dtos.OtpVerificationRequest;
import com.comp3334_t67.server.services.AuthService;
import com.comp3334_t67.server.services.RateLimitService;

import jakarta.servlet.http.HttpSession;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthControllerTest {

    private AuthService authService;
    private RateLimitService rateLimitService;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        rateLimitService = mock(RateLimitService.class);
        controller = new AuthController(authService, rateLimitService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_shouldCallRateLimitAndService() {
        AuthRequest req = new AuthRequest();
        req.setEmail("a@x.com");
        req.setPassword("pw");

        var response = controller.register(req);

        assertTrue(response.getBody().isSuccess());
        verify(rateLimitService).assertAllowed(startsWith("register:"), eq(10), any());
        verify(authService).register("a@x.com", "pw");
    }

    @Test
    void login_shouldSetOtpSessionAttributes() {
        AuthRequest req = new AuthRequest();
        req.setEmail("a@x.com");
        req.setPassword("pw");
        MockHttpServletRequest request = new MockHttpServletRequest();

        var response = controller.login(request, req);
        HttpSession session = request.getSession(false);

        assertNotNull(session);
        assertEquals("a@x.com", session.getAttribute("OTP_USER"));
        assertEquals(false, session.getAttribute("OTP_VERIFIED"));
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void verifyOtp_shouldAuthenticateWhenOtpValid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("OTP_USER", "a@x.com");
        when(authService.verifyOtp("a@x.com", 123456)).thenReturn(true);

        OtpVerificationRequest otp = new OtpVerificationRequest();
        otp.setOtp(123456);

        var response = controller.verifyOtp(request, otp);

        assertTrue(response.getBody().isSuccess());
        assertEquals(true, request.getSession(false).getAttribute("OTP_VERIFIED"));
        assertEquals("a@x.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    void verifyOtp_shouldThrowWhenOtpInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute("OTP_USER", "a@x.com");
        when(authService.verifyOtp("a@x.com", 111111)).thenReturn(false);

        OtpVerificationRequest otp = new OtpVerificationRequest();
        otp.setOtp(111111);

        assertThrows(OtpInvalidException.class, () -> controller.verifyOtp(request, otp));
    }
}
