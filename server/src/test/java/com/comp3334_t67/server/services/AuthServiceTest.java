package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.Exceptions.InvalidCredentialsException;
import com.comp3334_t67.server.Exceptions.UserAlreadyExistsException;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.UserRepository;

class AuthServiceTest {

    private UserRepository userRepo;
    private EmailService emailService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepo = mock(UserRepository.class);
        emailService = mock(EmailService.class);
        authService = new AuthService(userRepo, emailService);
    }

    @Test
    void register_shouldSaveWhenUserAbsent() {
        when(userRepo.findByEmail("a@x.com")).thenReturn(null);

        authService.register("a@x.com", "pw");

        verify(userRepo).save(any(User.class));
    }

    @Test
    void register_shouldThrowWhenDuplicate() {
        when(userRepo.findByEmail("a@x.com")).thenReturn(User.builder().email("a@x.com").build());

        assertThrows(UserAlreadyExistsException.class, () -> authService.register("a@x.com", "pw"));
    }

    @Test
    void login_shouldThrowWhenCredentialsInvalid() {
        when(userRepo.findByEmail("a@x.com")).thenReturn(null);

        assertThrows(InvalidCredentialsException.class, () -> authService.login("a@x.com", "pw"));
    }

    @Test
    void loginAndVerifyOtp_shouldSucceedThenConsumeOtp() {
        User user = User.builder().email("a@x.com").password("pw".getBytes()).build();
        when(userRepo.findByEmail("a@x.com")).thenReturn(user);

        final String[] sentOtp = new String[1];
        doAnswer(invocation -> {
            sentOtp[0] = invocation.getArgument(1, String.class);
            return null;
        }).when(emailService).sendOtpEmail(eq("a@x.com"), anyString());

        authService.login("a@x.com", "pw");

        assertNotNull(sentOtp[0]);
        int otp = Integer.parseInt(sentOtp[0]);
        assertTrue(authService.verifyOtp("a@x.com", otp));
        assertFalse(authService.verifyOtp("a@x.com", otp));
    }
}
