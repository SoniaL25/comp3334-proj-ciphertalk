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

    private String nouce;

    private String content_hashed;

    @Enumerated(EnumType.STRING)
    private MessageStatus status; // "sent", "delivered", "read"

    private LocalDateTime created_at;
    private LocalDateTime expires_at;
    private LocalDateTime delivered_at;
    private LocalDateTime read_at;

}
