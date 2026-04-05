package com.comp3334_t67.server.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

import com.comp3334_t67.server.Exceptions.RateLimitExceededException;
import com.comp3334_t67.server.models.RateLimit;
import com.comp3334_t67.server.repos.RateLimitRepository;

class RateLimitServiceTest {

    private RateLimitRepository rateLimitRepo;
    private RateLimitService service;

    @BeforeEach
    void setUp() {
        rateLimitRepo = mock(RateLimitRepository.class);
        service = new RateLimitService(rateLimitRepo);

        Map<String, RateLimit> store = new HashMap<>();

        when(rateLimitRepo.findByKey(any())).thenAnswer(invocation -> store.get(invocation.getArgument(0, String.class)));
        when(rateLimitRepo.save(any(RateLimit.class))).thenAnswer((Answer<RateLimit>) invocation -> {
            RateLimit rateLimit = invocation.getArgument(0, RateLimit.class);
            store.put(rateLimit.getKey(), rateLimit);
            return rateLimit;
        });
    }

    @Test
    void assertAllowed_shouldPermitWithinLimit() {
        assertDoesNotThrow(() -> service.assertAllowed("k1", 2, Duration.ofMinutes(1)));
    }

    @Test
    void assertAllowed_shouldThrowWhenLimitExceeded() {
        service.assertAllowed("k2", 1, Duration.ofMinutes(1));
        assertThrows(RateLimitExceededException.class,
            () -> service.assertAllowed("k2", 1, Duration.ofMinutes(1)));
    }
}
