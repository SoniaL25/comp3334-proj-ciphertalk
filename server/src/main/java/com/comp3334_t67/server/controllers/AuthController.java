package com.comp3334_t67.server.controllers;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.*;
import java.util.List;

import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.services.*;

import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Register
    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest.getEmail(), registerRequest.getPassword_hash());
        return "User registered successfully";
    }

    // login
    @PostMapping("/login")
    public String login(HttpServletRequest request, @RequestBody LoginRequest loginRequest) {
        authService.login(loginRequest.getEmail(), loginRequest.getPassword_hash());

        // Store temporary auth state in session
        HttpSession session = request.getSession();
        session.setAttribute("OTP_USER", loginRequest.getEmail());
        session.setAttribute("OTP_VERIFIED", false);

        return "OTP sent";
    }

    // verify OTP
    @PostMapping("/verify-otp")
    public String verifyOtp(HttpServletRequest request, @RequestBody OtpVerificationRequest otpRequest) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new IllegalStateException("No active session");
        }

        String email = (String) session.getAttribute("OTP_USER");
        if (email == null) {
            throw new IllegalStateException("No OTP verification in progress");
        }

        boolean isValid = authService.verifyOtp(email, otpRequest.getOtp());
        if (!isValid) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        // Mark OTP as verified
        session.setAttribute("OTP_VERIFIED", true);
        request.changeSessionId();

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(email, null, List.of());

        SecurityContextHolder.getContext().setAuthentication(auth);
        
        return "OTP verified, login successful";

    }

    // logout
    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
        return "Logged out successfully";
    }

}
