package com.comp3334_t67.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.*;
import java.time.Duration;
import java.util.List;

import com.comp3334_t67.server.Exceptions.OtpInvalidException;
import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.services.*;

import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final int AUTH_EXPIRY_MINUTES = 10;
    private final int AUTH_MAX_ATTEMPTS = 5;

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    //health check
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Void>> healthCheck() {  
        return ResponseEntity.ok(ApiResponse.success("Server is healthy", null));
    }

    // Register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(HttpServletRequest request, @RequestBody AuthRequest registerRequest) {
        
        String ip = request.getRemoteAddr();
        log.info("Register request received from ip={}", ip);

        // Apply rate limiting to registration endpoint
        String key = "register:ip:" + ip;
        rateLimitService.assertAllowed(key, AUTH_MAX_ATTEMPTS, Duration.ofMinutes(AUTH_EXPIRY_MINUTES));

        // Register the user
        authService.register(registerRequest.getEmail(), registerRequest.getPassword());
        log.info("Register request succeeded from ip={}", ip);
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", null));
    }

    // temp login without otp for testing
    @PostMapping("/temp-login")
    public ResponseEntity<ApiResponse<Void>> tempLogin(HttpServletRequest request, @RequestBody AuthRequest loginRequest) {
        String ip = request.getRemoteAddr();
        log.info("Temp login request received from ip={}", ip);
        authService.login(loginRequest.getEmail(), loginRequest.getPassword());
        // Store temporary auth state in session
        HttpSession session = request.getSession();
        session.setAttribute("OTP_USER", loginRequest.getEmail());
        // Mark OTP as verified
        session.setAttribute("OTP_VERIFIED", true);
        request.changeSessionId();

        // Set authentication in security context (user, credentials, authorities)
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), null, List.of());

        // store auth in current context
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.info("TEMP LOGIN: Fake OTP verification succeeded from ip={}", ip);
        
        return ResponseEntity.ok(ApiResponse.success("TEMP LOGIN: Fake OTP verified, login successful", null));
    
    }


    // login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(HttpServletRequest request, @RequestBody AuthRequest loginRequest) {
        String ip = request.getRemoteAddr();
        log.info("Login request received from ip={}", ip);
        String key = "login:ip:" + ip;
        rateLimitService.assertAllowed(key, AUTH_MAX_ATTEMPTS, Duration.ofMinutes(AUTH_EXPIRY_MINUTES));

        authService.login(loginRequest.getEmail(), loginRequest.getPassword());

        // Store temporary auth state in session
        HttpSession session = request.getSession();
        session.setAttribute("OTP_USER", loginRequest.getEmail());
        session.setAttribute("OTP_VERIFIED", false);

        log.info("Login request accepted and OTP flow started from ip={}", ip);

        return ResponseEntity.ok(ApiResponse.success("OTP sent", null));
    }

    // verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(HttpServletRequest request, @RequestBody OtpVerificationRequest otpRequest) {
        String ip = request.getRemoteAddr();
        log.info("OTP verification request received from ip={}", ip);

        boolean isValid = authService.verifyOtp(otpRequest.getEmail(), otpRequest.getOtp());
        if (!isValid) {
            log.warn("OTP verification failed: invalid OTP from ip={}", ip);
            throw new OtpInvalidException("Invalid OTP");
        }

        // Create or reuse session after OTP success for subsequent protected endpoints.
        HttpSession session = request.getSession(true);
        session.setAttribute("OTP_USER", otpRequest.getEmail());
        session.setAttribute("OTP_VERIFIED", true);
        request.changeSessionId();

        // Set authentication in security context (user, credentials, authorities)
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(otpRequest.getEmail(), null, List.of());

        // store auth in current context
        SecurityContextHolder.getContext().setAuthentication(auth);
        log.info("OTP verification succeeded from ip={}", ip);
        
        return ResponseEntity.ok(ApiResponse.success("OTP verified, login successful", null));

    }

    // logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        log.info("Logout request received from ip={}", ip);
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        log.info("Logout request succeeded from ip={}", ip);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

}
