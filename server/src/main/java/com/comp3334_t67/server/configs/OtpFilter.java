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

        HttpSession session = request.getSession(false);

        if (session.equals(null)) {
            Boolean verified = (Boolean) session.getAttribute("OTP_VERIFIED");

            String path = request.getRequestURI();

            if (Boolean.FALSE.equals(verified)
                && !path.startsWith("/api/auth/verify-otp")
                && !path.startsWith("/api/auth/login")
                && !path.startsWith("/api/auth/register")) {

                response.sendError(403, "OTP not verified");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
