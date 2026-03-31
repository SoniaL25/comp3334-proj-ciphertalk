package com.comp3334_t67.server.models;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;
import java.util.*;


@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "blocked_users",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "blocked_user_id"})
)    
public class BlockedUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "blocked_user_id")
    private UUID blockedUserId;

    private LocalDateTime blocked_at;
    
}
