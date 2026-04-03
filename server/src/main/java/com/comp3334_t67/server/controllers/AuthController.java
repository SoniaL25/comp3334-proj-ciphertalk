package com.comp3334_t67.server.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.*;
import java.time.Duration;
import java.util.List;

import com.comp3334_t67.server.Exceptions.OtpInvalidException;
import com.comp3334_t67.server.Exceptions.OtpSessionMissingException;
import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.services.*;

import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final int RATE_LIMIT_WINDOW = 5;
    private final int RATE_LIMIT_LIMIT = 10;

    private final AuthService authService;
    private final RateLimitService rateLimitService;

    //health check
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {  
        return ResponseEntity.ok(ApiResponse.success("Server is healthy", null));
    }

    // Register
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(@RequestBody AuthRequest registerRequest) {
        String key = "register:" + registerRequest.getEmail();
        rateLimitService.assertAllowed(key, RATE_LIMIT_LIMIT, Duration.ofMinutes(RATE_LIMIT_WINDOW));

        authService.register(registerRequest.getEmail(), registerRequest.getPasswordHash());
        return ResponseEntity.ok(ApiResponse.success("User registered successfully", null));
    }

    // login
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Void>> login(HttpServletRequest request, @RequestBody AuthRequest loginRequest) {
        String key = "login:" + loginRequest.getEmail();
        rateLimitService.assertAllowed(key, RATE_LIMIT_LIMIT, Duration.ofMinutes(RATE_LIMIT_WINDOW));

        authService.login(loginRequest.getEmail(), loginRequest.getPasswordHash());

        // Store temporary auth state in session
        HttpSession session = request.getSession();
        session.setAttribute("OTP_USER", loginRequest.getEmail());
        session.setAttribute("OTP_VERIFIED", false);

        return ResponseEntity.ok(ApiResponse.success("OTP sent", null));
    }

    // verify OTP
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(HttpServletRequest request, @RequestBody OtpVerificationRequest otpRequest) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new OtpSessionMissingException("No active session");
        }

        String email = (String) session.getAttribute("OTP_USER");
        if (email == null) {
            throw new OtpSessionMissingException("No OTP verification in progress");
        }

        boolean isValid = authService.verifyOtp(email, otpRequest.getOtp());
        if (!isValid) {
            throw new OtpInvalidException("Invalid OTP");
        }

        // Mark OTP as verified
        session.setAttribute("OTP_VERIFIED", true);
        request.changeSessionId();

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(email, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(auth);
        
        return ResponseEntity.ok(ApiResponse.success("OTP verified, login successful", null));

    }

    // logout
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Object>> logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

}
