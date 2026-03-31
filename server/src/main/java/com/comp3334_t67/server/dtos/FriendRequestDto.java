package com.comp3334_t67.server.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.comp3334_t67.server.enums.FriendRequestStatus;

import lombok.Data;

@Data
public class FriendRequestDto {

    private UUID senderId;
    private UUID receiverId;
    private FriendRequestStatus status; // "pending", "accepted", "rejected", "canceled"
    private LocalDateTime created_at;
}
