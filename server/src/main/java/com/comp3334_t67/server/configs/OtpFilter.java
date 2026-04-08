package com.comp3334_t67.server.configs;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;


@Component
public class OtpFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
                                   throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean isAuthPath = path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")
                || path.startsWith("/api/auth/verify-otp")
                || path.startsWith("/api/auth/temp-login")
                || path.startsWith("/api/auth/health");

        // Auth endpoints are always allowed; OTP checks are for protected routes only.
        if (isAuthPath) {
            filterChain.doFilter(request, response);
            return;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            response.sendError(401, "No active session");
            return;
        }

        Boolean verified = (Boolean) session.getAttribute("OTP_VERIFIED");
        String email = (String) session.getAttribute("OTP_USER");
        if (!Boolean.TRUE.equals(verified) || email == null || email.isBlank()) {
            response.sendError(403, "OTP not verified");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
