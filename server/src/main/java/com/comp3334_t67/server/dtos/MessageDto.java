package com.comp3334_t67.server.dtos;

import java.time.LocalDateTime;
import java.util.UUID;

import com.comp3334_t67.server.enums.MessageStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDto {
    private UUID chatId;
    private UUID senderId;
    private String content;
    private String nonce;
    private String clientMessageId;
    private String tag;
    private LocalDateTime sentAt;
    private MessageStatus status;
}
