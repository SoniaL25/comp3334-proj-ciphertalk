package com.comp3334_t67.server.models;

import java.util.UUID;

import com.comp3334_t67.server.enums.FriendRequestStatus;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "friend_requests")
public class FriendRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "receiver_id")
    private UUID receiverId;

    @Enumerated(EnumType.STRING)
    private FriendRequestStatus status; // "pending", "accepted", "rejected", "canceled"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
