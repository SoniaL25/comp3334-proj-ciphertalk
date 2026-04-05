package com.comp3334_t67.server.dtos;

import java.time.LocalDateTime;

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
    private String content;
    private LocalDateTime sentAt;
    private MessageStatus status;
}
