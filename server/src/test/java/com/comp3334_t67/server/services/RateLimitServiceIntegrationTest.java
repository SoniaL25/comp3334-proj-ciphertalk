package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.comp3334_t67.server.IntegrationTestBase;
import com.comp3334_t67.server.Exceptions.RateLimitExceededException;

@SpringBootTest
class RateLimitServiceIntegrationTest extends IntegrationTestBase {

    @Autowired
    private RateLimitService rateLimitService;

    @Test
    void assertAllowed_shouldPassWithinLimit() {
        // Arrange + Act + Assert: first two attempts within limit are allowed.
        assertDoesNotThrow(() -> rateLimitService.assertAllowed("k1", 2, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> rateLimitService.assertAllowed("k1", 2, Duration.ofMinutes(1)));
    }

    @Test
    void assertAllowed_shouldThrowWhenLimitExceeded() {
        // Arrange: consume first allowed attempt.
        rateLimitService.assertAllowed("k2", 1, Duration.ofMinutes(1));

        // Act + Assert: next attempt exceeds limit.
        assertThrows(RateLimitExceededException.class, () -> rateLimitService.assertAllowed("k2", 1, Duration.ofMinutes(1)));
    }

    @Test
    void assertAllowedWithLockout_shouldThrowAfterExceedingLimit() {
        // Arrange: first attempt is allowed.
        rateLimitService.assertAllowedWithLockout("lock-key", 1, Duration.ofMinutes(1));

        // Act + Assert: second attempt triggers lockout.
        assertThrows(RateLimitExceededException.class, () -> rateLimitService.assertAllowedWithLockout("lock-key", 1, Duration.ofMinutes(1)));
    }
}
