package com.comp3334_t67.server.models;

import java.util.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "messages")
public class RateLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String key;

    private int attempts;

    private long windowStart; // timestamp of the start of the current window

    private long lockoutUntil; // timestamp until which the key is locked out

}
