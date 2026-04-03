package com.comp3334_t67.server.services;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.comp3334_t67.server.Exceptions.InvalidCredentialsException;
import com.comp3334_t67.server.Exceptions.UserAlreadyExistsException;
import com.comp3334_t67.server.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.comp3334_t67.server.repos.*;



@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final EmailService emailService;

    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> expiryStore = new ConcurrentHashMap<>();

    // Register a new user
    public void register(String email, String passwordHash) { 

        // check if user already exists
        if (userRepo.findByEmail(email) != null) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        // create a new user with the provided email and password hash
        User user = createUser(email, passwordHash);
        // save the user to the database
        userRepo.save(user);

    }

    public String login(String email, String passwordHash) {

        // verify user credentials
        if (!verifyCredentials(email, passwordHash)) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // generate OTP and store it in memory with an expiry time
        int otp = generateOTP();
        otpStore.put(email, String.valueOf(otp));
        expiryStore.put(email, System.currentTimeMillis() + 300000); // 5 minutes expiry

        // send OTP to user's email
        emailService.sendOtpEmail(email, String.valueOf(otp));

        // return a message indicating that OTP has been sent
        return "OTP has been sent to your email";

    }

    public boolean verifyOtp(String email, int otp) {
        
        // check if OTP exists for the email
        if (otpStore.containsKey(email) && expiryStore.containsKey(email)) {

            // check if OTP is still valid
            if (System.currentTimeMillis() < expiryStore.get(email)) {
                boolean isValid = otpStore.get(email).equals(String.valueOf(otp));
                if (isValid) {
                    otpStore.remove(email);
                    expiryStore.remove(email);
                }
                return isValid;
            } else {

                // OTP expired, remove from store
                otpStore.remove(email);
                expiryStore.remove(email);
            }
        }
        return false;
    }

    

    // HELPER METHODS ========================

    // create new user
    private User createUser(String email, String passwordHash) {
        User user = User.builder()
                        .email(email)
                        .passwordHash(passwordHash.getBytes())
                        .build();
        return user;
    }

    // verify user credentials
    public boolean verifyCredentials(String email, String passwordHash) {
        User user = userRepo.findByEmail(email);
        if (user != null) {
            return new String(user.getPasswordHash()).equals(passwordHash);
        }
        return false;
    }

    // OTP generator
    private int generateOTP() {
        // generate a random 6-digit OTP
        int otp = (int)(Math.random() * 900000) + 100000;
        return otp;
    }
    
}
