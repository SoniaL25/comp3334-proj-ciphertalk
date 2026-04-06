package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void sendOtpEmail_shouldSendExpectedMessage() {
        // Arrange: prepare input payload.
        String email = "MAIL@EXAMPLE.COM";
        String otp = "123456";

        // Act: send OTP message.
        emailService.sendOtpEmail(email, otp);

        // Assert: JavaMail sender receives expected content.
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals(email, captor.getValue().getTo()[0]);
        assertEquals("CipherTalk - Your OTP Code", captor.getValue().getSubject());
    }
}
