package com.comp3334_t67.server.models;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rate_limits")
public class RateLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "rate_key", nullable = false, unique = true)
    private String key;

    @Column(name = "attempts", nullable = false)
    private int attempts;

    @Column(name = "window_start", nullable = false)
    private long windowStart; // timestamp of the start of the current window

    @Column(name = "lockout_until", nullable = false)
    private long lockoutUntil; // timestamp until which the key is locked out

}
