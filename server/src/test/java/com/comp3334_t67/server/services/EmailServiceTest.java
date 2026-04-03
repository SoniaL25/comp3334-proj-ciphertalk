package com.comp3334_t67.server.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private EmailService service;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        service = new EmailService();
        ReflectionTestUtils.setField(service, "mailSender", mailSender);
    }

    @Test
    void sendOtpEmail_shouldSendMailMessage() {
        service.sendOtpEmail("a@x.com", "123456");

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
    }
}
