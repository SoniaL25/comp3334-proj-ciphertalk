package com.comp3334_t67.server.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendChatDto {
    private UUID chatId;
    private UUID senderId;
    private UUID receiverId;
    private String friendEmail;
    private long numOfUnreadMessage;
    private LocalDateTime lastMessageDateTime;
}