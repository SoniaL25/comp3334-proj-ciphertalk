package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.comp3334_t67.server.Exceptions.RateLimitExceededException;

class RateLimitServiceTest {

    private final RateLimitService service = new RateLimitService();

    @Test
    void assertAllowed_shouldPermitWithinLimit() {
        assertDoesNotThrow(() -> service.assertAllowed("k1", 2, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> service.assertAllowed("k1", 2, Duration.ofMinutes(1)));
    }

    @Test
    void assertAllowed_shouldThrowWhenLimitExceeded() {
        service.assertAllowed("k2", 1, Duration.ofMinutes(1));
        assertThrows(RateLimitExceededException.class,
            () -> service.assertAllowed("k2", 1, Duration.ofMinutes(1)));
    }
}
