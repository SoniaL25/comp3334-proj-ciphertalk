package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

@SpringBootTest
class EmailServiceIntegrationTest {

    @Autowired
    private EmailService emailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @Test
    void sendOtpEmail_shouldSendExpectedMessage() throws Exception {
        // Arrange: prepare input payload.
        String email = "MAIL@EXAMPLE.COM";
        String otp = "123456";
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act: send OTP message.
        emailService.sendOtpEmail(email, otp);

        // Assert: JavaMail sender receives expected content.
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals(email, captor.getValue().getAllRecipients()[0].toString());
        assertEquals("CipherTalk - Your OTP Code", captor.getValue().getSubject());
    }
}
