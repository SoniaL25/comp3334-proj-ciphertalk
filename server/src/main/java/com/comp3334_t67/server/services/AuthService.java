package com.comp3334_t67.server.services;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.comp3334_t67.server.Exceptions.*;
import com.comp3334_t67.server.models.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.time.Duration;

import com.comp3334_t67.server.repos.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepo;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;

    private final Map<String, String> otpStore = new ConcurrentHashMap<>();
    private final Map<String, Long> expiryStore = new ConcurrentHashMap<>();

    private final int LOGIN_LOCKOUT_DURATION = 15;
    private final int LOGIN_MAX_ATTEMPTS = 10;

    // Register a new user
    public void register(String email, String password) { 
        log.info("Register attempt received");

        // check if user already exists
        if (userRepo.findByEmail(email) != null) {
            log.warn("Register rejected: user already exists");
            throw new UserAlreadyExistsException("User with this email already exists");
        }

        // validate email format
        if(!isValidEmail(email)) {
            log.warn("Register rejected: invalid email format");
            throw new InvalidInputException("Invalid email format");
        }

        // create a new user with the provided email and password hash
        User user = createUser(email, password);

        // save the user to the database
        userRepo.save(user);
        log.info("Register completed successfully");

    }

    public String login(String email, String password) {
        log.info("Login attempt received");

        // verify email
        User user = userRepo.findByEmail(email);
        if (user == null) {
            log.warn("Login rejected: user not found");
            throw new InvalidCredentialsException("Invalid email or password");            
        }

        // verify password and apply rate limiting with lockout on failed attempts
        if (!passwordEncoder.matches(password, new String(user.getPassword()))) {
            log.warn("Login rejected: invalid credentials");
            
            String key = "login:user:" + email;
            rateLimitService.assertAllowedWithLockout(key, LOGIN_MAX_ATTEMPTS, Duration.ofMinutes(LOGIN_LOCKOUT_DURATION));
            throw new InvalidCredentialsException("Invalid email or password");
        }
        
        // generate OTP and store it in memory with an expiry time
        int otp = generateOTP();
        otpStore.put(email, passwordEncoder.encode(String.valueOf(otp)));
        expiryStore.put(email, System.currentTimeMillis() + 300000); // 5 minutes expiry

        // send OTP to user's email
        emailService.sendOtpEmail(email, String.valueOf(otp));
        log.info("Login accepted and OTP generated");

        // return a message indicating that OTP has been sent
        return "OTP has been sent to your email";

    }

    public boolean verifyOtp(String email, int otp) {
        log.info("OTP verification attempt received");
        
        // check if OTP exists for the email
        if (otpStore.containsKey(email) && expiryStore.containsKey(email)) {

            // check if OTP is still valid (ie not expired)
            if (System.currentTimeMillis() < expiryStore.get(email)) {
                // verify OTP is correct
                boolean isValid = passwordEncoder.matches(String.valueOf(otp), otpStore.get(email));
                
                // If OTP is valid, remove it from the store to prevent reuse
                if (isValid) {
                    otpStore.remove(email);
                    expiryStore.remove(email);
                    log.info("OTP verification succeeded");
                } else {
                    log.warn("OTP verification failed: mismatch");
                }

                // return whether OTP is valid or not
                return isValid;
            } else {

                // OTP expired, remove from store
                otpStore.remove(email);
                expiryStore.remove(email);
                log.warn("OTP verification failed: OTP expired");
            }
        }
        log.warn("OTP verification failed: OTP state not found");
        return false;
    }

    // HELPER METHODS ========================

    // create new user
    private User createUser(String email, String password) {

        // hash the password before storing (using Spring's PasswordEncoder, per-user salting)
        String hashedPassword = passwordEncoder.encode(password);

        User user = User.builder()
                        .email(email)
                        .password(hashedPassword.getBytes())
                        .build();
        return user;
    }

    // OTP generator
    private int generateOTP() {
        // generate a random 6-digit OTP
        int otp = (int)(Math.random() * 900000) + 100000;
        return otp;
    }

    // Email Validation
    public boolean isValidEmail(String email) {
        String emailRegex =  "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        Pattern pat = Pattern.compile(emailRegex, Pattern.CASE_INSENSITIVE);
        return pat.matcher(email).matches();
    }
    
}
