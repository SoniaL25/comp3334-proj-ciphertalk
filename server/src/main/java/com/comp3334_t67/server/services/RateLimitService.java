package com.comp3334_t67.server.services;

import org.springframework.stereotype.Service;
import com.comp3334_t67.server.Exceptions.RateLimitExceededException;
import com.comp3334_t67.server.repos.RateLimitRepository;
import com.comp3334_t67.server.models.RateLimit;

import lombok.AllArgsConstructor;

import java.time.*;

@Service
@AllArgsConstructor
public class RateLimitService {

    private final RateLimitRepository rateLimitRepo;

    // Basic rate limiting without lockout
    public void assertAllowed(String key, int limit, Duration window) {

        // Keep only timestamps within the current window
        long now = System.currentTimeMillis();

        // find rate limit for the key, if not exist, create a new one
        RateLimit rateLimit = getOrCreateRateLimit(key);

        // get time difference from window start
        long diff = now - rateLimit.getWindowStart();

        if (diff > window.toMillis()) {
            // Window has passed, reset attempts and window start
            rateLimit.setAttempts(1);
            rateLimit.setWindowStart(now);
            rateLimitRepo.save(rateLimit);

        } else {
            // Increment attempts and check limit
            rateLimit.setAttempts(rateLimit.getAttempts() + 1);
            if (rateLimit.getAttempts() > limit) {
                rateLimitRepo.save(rateLimit);
                throw new RateLimitExceededException("Too many attempts. Try again later.");
            }
            rateLimitRepo.save(rateLimit);
        }

    }

    // Rate limiting with lockout mechanism
    public void assertAllowedWithLockout(String key, int limit, Duration lockout) {
        // get current timestamp
        long now = System.currentTimeMillis();

        // find rate limit for the key, if not exist, create a new one
        RateLimit rateLimit = getOrCreateRateLimit(key);
        
        // Case 1: If currently locked out, throw exception
        if (rateLimit.getLockoutUntil() > now) {
            throw new RateLimitExceededException("Too many attempts. Try again later.");
        }

        // Case 2: lockout passed, reset attempts and window start
        else if (rateLimit.getLockoutUntil() != 0 && rateLimit.getLockoutUntil() <= now) {
            rateLimit.setAttempts(1);
            rateLimit.setWindowStart(now);
            rateLimit.setLockoutUntil(0);
            rateLimitRepo.save(rateLimit);
        }

        // Case 3: lockout until == 0: Increment attempts and check limit, 
        else if (rateLimit.getLockoutUntil() == 0) {
            rateLimit.setAttempts(rateLimit.getAttempts() + 1);
            if (rateLimit.getAttempts() > limit) {
                // Case 4: limit exceeded: set lockout until and save
                rateLimit.setLockoutUntil(now + lockout.toMillis());
                rateLimitRepo.save(rateLimit);
                throw new RateLimitExceededException("Too many attempts. Try again later.");
            }
            rateLimitRepo.save(rateLimit);
        }

    }

    // HELPER METHOD
    
    // Get the RateLimit for the given key, or create a new one if it doesn't exist
    private RateLimit getOrCreateRateLimit(String key) {
        RateLimit rateLimit = rateLimitRepo.findByKey(key);
        if (rateLimit == null) {
            rateLimit = RateLimit.builder()
                    .key(key)
                    .attempts(0)
                    .windowStart(System.currentTimeMillis())
                    .lockoutUntil(0)
                    .build();
            rateLimitRepo.save(rateLimit);
        }
        return rateLimit;
    }

}
