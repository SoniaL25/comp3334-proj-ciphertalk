package com.comp3334_t67.server.models;

import java.util.UUID;

import com.comp3334_t67.server.enums.MessageStatus;

import jakarta.persistence.*;
import lombok.*;
import java.time.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id")
    private UUID chatId;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(name = "receiver_id")
    private UUID receiverId;

    @Column(name = "nonce")
    private String nonce;

    @Column(name = "content_hashed")
    private String contentHashed;

    @Enumerated(EnumType.STRING)
    private MessageStatus status; // "sent", "delivered", "read"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

}
