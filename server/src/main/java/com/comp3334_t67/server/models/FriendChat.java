package com.comp3334_t67.server.models;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;
import java.time.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "friend_chats",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
)
public class FriendChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user1_id")
    private UUID user1Id;

    @Column(name = "user2_id")
    private UUID user2Id;

    private LocalDateTime created_at;

}
