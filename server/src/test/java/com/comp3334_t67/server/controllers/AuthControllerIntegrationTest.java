package com.comp3334_t67.server.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.Exceptions.OtpInvalidException;
import com.comp3334_t67.server.Exceptions.OtpSessionMissingException;
import com.comp3334_t67.server.dtos.AuthRequest;
import com.comp3334_t67.server.dtos.OtpVerificationRequest;
import com.comp3334_t67.server.services.AuthService;
import com.comp3334_t67.server.services.EmailService;
import com.comp3334_t67.server.services.RateLimitService;

@SpringBootTest
class AuthControllerIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthController controller;

    @MockitoSpyBean
    private AuthService authService;

    @MockitoSpyBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private EmailService emailService;

    @Test
    void register_shouldSucceed_andVerifyInteractions() {
        // Arrange: build request and mock servlet request.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        AuthRequest body = new AuthRequest();
        body.setEmail("REG@EXAMPLE.COM");
        body.setPassword("pw");

        // Act: call register endpoint.
        var response = controller.register(request, body);

        // Assert: response success and service interactions happen.
        assertTrue(response.getBody().isSuccess());
        verify(rateLimitService).assertAllowed(org.mockito.ArgumentMatchers.contains("register:ip:"), org.mockito.ArgumentMatchers.eq(5), org.mockito.ArgumentMatchers.any());
        verify(authService).register("REG@EXAMPLE.COM", "pw");
    }

    @Test
    void loginAndVerifyOtp_shouldSucceed_withSessionFlow() {

        String mockEmail = "sonia.lim.2023@scis.smu.edu.sg";
        // Arrange: register user and call login first.
        MockHttpServletRequest loginRequest = new MockHttpServletRequest();
        loginRequest.setRemoteAddr("127.0.0.1");
        AuthRequest reg = new AuthRequest();
        reg.setEmail(mockEmail);
        reg.setPassword("pw");
        controller.register(loginRequest, reg);

        AuthRequest loginBody = new AuthRequest();
        loginBody.setEmail(mockEmail);
        loginBody.setPassword("pw");
        var loginResponse = controller.login(loginRequest, loginBody);

        // Assert: login creates OTP flow.
        assertTrue(loginResponse.getBody().isSuccess());
        verify(emailService).sendOtpEmail(org.mockito.ArgumentMatchers.eq(mockEmail), org.mockito.ArgumentMatchers.anyString());

        // Arrange: capture otp value and build verify request.
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(org.mockito.ArgumentMatchers.eq(mockEmail), otpCaptor.capture());
        OtpVerificationRequest otpRequest = new OtpVerificationRequest();
        otpRequest.setOtp(Integer.parseInt(otpCaptor.getValue()));

        // Act: verify otp using same HTTP session.
        var verifyResponse = controller.verifyOtp(loginRequest, otpRequest);

        // Assert: verification succeeds.
        assertTrue(verifyResponse.getBody().isSuccess());
        assertEquals("OTP verified, login successful", verifyResponse.getBody().getMessage());
    }

    @Test
    void verifyOtp_shouldThrow_whenNoSessionExists() {
        // Arrange: create request without session.
        MockHttpServletRequest request = new MockHttpServletRequest();
        OtpVerificationRequest otpRequest = new OtpVerificationRequest();
        otpRequest.setOtp(123456);

        // Act + Assert: missing session is rejected.
        assertThrows(OtpSessionMissingException.class, () -> controller.verifyOtp(request, otpRequest));
    }

    @Test
    void verifyOtp_shouldThrow_whenOtpIsInvalid() {
        // Arrange: start session with OTP_USER but without generated OTP state.
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute("OTP_USER", "MISSING@EXAMPLE.COM");
        OtpVerificationRequest otpRequest = new OtpVerificationRequest();
        otpRequest.setOtp(111111);

        // Act + Assert: invalid otp path throws expected exception.
        assertThrows(OtpInvalidException.class, () -> controller.verifyOtp(request, otpRequest));
    }

    @Test
    void logout_shouldInvalidateSession() {
        // Arrange: create request with an active session.
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);

        // Act: log out.
        var response = controller.logout(request);

        // Assert: response is successful and session is invalidated.
        assertTrue(response.getBody().isSuccess());
        assertTrue(session.isInvalid());
    }
}
