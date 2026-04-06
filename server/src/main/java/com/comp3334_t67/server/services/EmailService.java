package com.comp3334_t67.server.services;

import com.comp3334_t67.server.Exceptions.EmailDeliveryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    // send OTP to user's email
    public void sendOtpEmail(String email, String otp) {
        log.info("Sending OTP email");

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(email);
            helper.setFrom(new InternetAddress("sonialimje25@gmail.com", "CipherTalk OTP Service"));
            helper.setSubject("CipherTalk - Your OTP Code");
            helper.setText("Your OTP code is: " + otp + "\nThis code will expire in 5 minutes.");

            mailSender.send(message);
            log.info("OTP email send succeeded");
        } catch (MailException ex) {
            log.error("OTP email send failed: {}", ex.getMessage());
            throw new EmailDeliveryException("Unable to send OTP email right now. Please try again later.");
        } catch (Exception ex) {
            log.error("OTP email build/send failed: {}", ex.getMessage());
            throw new EmailDeliveryException("Unable to send OTP email right now. Please try again later.");
        }
    }

    
}
