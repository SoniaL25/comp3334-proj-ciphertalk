package com.comp3334_t67.server.services;

import org.springframework.stereotype.Service;
import com.comp3334_t67.server.Exceptions.RateLimitExceededException;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, Deque<Long>> bucket = new ConcurrentHashMap<>();

    public void assertAllowed(String key, int limit, Duration window) {
        // Keep only timestamps within the current window
        long now = System.currentTimeMillis();
        // calculate the start of the window (within duration window)
        long windowStart = now - window.toMillis();

        // Get the deque of timestamps for the given key, or create a new one if it doesn't exist
        Deque<Long> timestamps = bucket.computeIfAbsent(key, unused -> new ArrayDeque<>());

        synchronized (timestamps) {
            // Remove timestamps that are outside the current window
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            // Check if the number of timestamps in the current window exceeds the limit
            if (timestamps.size() >= limit) {
                throw new RateLimitExceededException("Rate limit exceeded. Please try again later.");
            }
            
            // Add the current timestamp to the deque
            timestamps.addLast(now);
        }
    }
}
