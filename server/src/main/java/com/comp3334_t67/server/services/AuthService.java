package com.comp3334_t67.server.services;


import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.comp3334_t67.server.Exceptions.InvalidCredentialsException;
import com.comp3334_t67.server.Exceptions.InvalidInputException;
import com.comp3334_t67.server.Exceptions.UserAlreadyExistsException;
import com.comp3334_t67.server.models.User;
import com.comp3334_t67.server.repos.UserRepository;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepo;
    private final EmailService emailService;
    private final RateLimitService rateLimitService;
    private final PasswordEncoder passwordEncoder;

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
        
        // generate OTP and store it in database with an expiry time
        int otp = generateOTP();
        user.setOtpSecret(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5)); // OTP valid for 5 minutes
        userRepo.save(user);

        // send OTP to user's email
        emailService.sendOtpEmail(email, String.valueOf(otp));
        log.info("Login accepted and OTP generated");

        // return a message indicating that OTP has been sent
        return "OTP has been sent to your email";

    }

    public boolean verifyOtp(String email, int otp) {
        log.info("OTP verification attempt received");
        
        // check if OTP exists for the email
        User user = userRepo.findByEmail(email);
        if (user == null) {
            log.warn("OTP verification failed: user not found");
            return false;
        }
        else if (user.getOtpSecret() != 0) { // check if OTP is set for the user
            // check if OTP is still valid (ie not expired)
            if (user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now())) {
                // verify OTP is correct
                boolean isValid = user.getOtpSecret() == otp;
                
                // If OTP is valid, remove it from the store to prevent reuse
                if (isValid) {
                    user.setOtpSecret(0);
                    user.setOtpExpiry(LocalDateTime.now());
                    userRepo.save(user);
                    log.info("OTP verification succeeded");
                } else {
                    log.warn("OTP verification failed: mismatch");
                }

                // return whether OTP is valid or not
                return isValid;
            } else {

                // OTP expired, remove from store
                user.setOtpSecret(0);
                user.setOtpExpiry(LocalDateTime.now());
                userRepo.save(user);
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
        // Generate a cryptographically secure 6-digit OTP in [100000, 999999].
        return SECURE_RANDOM.nextInt(900_000) + 100_000;
    }

    // Email Validation
    public boolean isValidEmail(String email) {
        String emailRegex =  "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$";
        Pattern pat = Pattern.compile(emailRegex, Pattern.CASE_INSENSITIVE);
        return pat.matcher(email).matches();
    }
    
}
