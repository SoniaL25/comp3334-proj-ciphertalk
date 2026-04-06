package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.Exceptions.InvalidCredentialsException;
import com.comp3334_t67.server.Exceptions.InvalidInputException;
import com.comp3334_t67.server.Exceptions.UserAlreadyExistsException;
import com.comp3334_t67.server.models.User;

@SpringBootTest
class AuthServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoSpyBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private EmailService emailService;

    @Test
    void register_shouldPersistUser_whenInputIsValid() {
        // Arrange: prepare a valid uppercase email that matches current regex behavior.
        String email = "ALICE@EXAMPLE.COM";

        // Act: register a new user.
        assertDoesNotThrow(() -> authService.register(email, "secret"));

        // Assert: user exists in real database.
        assertTrue(userRepo.findByEmail(email) != null);
    }

    @Test
    void register_shouldThrow_whenDuplicateUserExists() {
        // Arrange: seed user first.
        userRepo.save(User.builder().email("BOB@EXAMPLE.COM").password("x".getBytes()).build());

        // Act + Assert: duplicate registration fails.
        assertThrows(UserAlreadyExistsException.class, () -> authService.register("BOB@EXAMPLE.COM", "secret"));
    }

    @Test
    void register_shouldThrow_whenEmailFormatIsInvalid() {
        // Arrange: use lowercase format that current regex rejects.
        String invalidEmail = "bob@example.com";

        // Act + Assert: invalid format is rejected.
        assertThrows(InvalidInputException.class, () -> authService.register(invalidEmail, "secret"));
    }

    @Test
    void login_shouldThrowAndRateLimit_whenPasswordIsInvalid() {
        // Arrange: insert user with known password hash.
        String email = "CAROL@EXAMPLE.COM";
        userRepo.save(User.builder().email(email).password(passwordEncoder.encode("right-pass").getBytes()).build());

        // Act + Assert: wrong password throws credentials error.
        assertThrows(InvalidCredentialsException.class, () -> authService.login(email, "wrong-pass"));

        // Assert: lockout counter path is invoked.
        verify(rateLimitService).assertAllowedWithLockout(eq("login:user:" + email), eq(10), any(Duration.class));
    }

    @Test
    void loginAndVerifyOtp_shouldSucceed_onceThenFailOnReuse() {
        // Arrange: persist user and capture OTP sent by service.
        String email = "DAVE@EXAMPLE.COM";
        userRepo.save(User.builder().email(email).password(passwordEncoder.encode("pw").getBytes()).build());

        // Act: login to trigger OTP generation.
        String loginResult = authService.login(email, "pw");

        // Assert: login response text and email dispatch behavior are correct.
        assertEquals("OTP has been sent to your email", loginResult);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(email), otpCaptor.capture());

        // Act: verify using the generated OTP.
        int otp = Integer.parseInt(otpCaptor.getValue());
        boolean firstTry = authService.verifyOtp(email, otp);
        boolean secondTry = authService.verifyOtp(email, otp);

        // Assert: OTP works once and cannot be reused.
        assertTrue(firstTry);
        assertFalse(secondTry);
    }

    @Test
    void isValidEmail_shouldReflectCurrentRegexBehavior() {
        // Arrange + Act: evaluate one valid and one invalid format.
        boolean valid = authService.isValidEmail("EVE@EXAMPLE.COM");
        boolean invalid = authService.isValidEmail("eve@example.com");

        // Assert: uppercase format is accepted while lowercase is rejected.
        assertTrue(valid);
        assertFalse(invalid);
    }
}
