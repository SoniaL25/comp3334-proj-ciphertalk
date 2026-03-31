package com.comp3334_t67.server.dtos;

import lombok.Data;
import java.util.UUID;

@Data
public class CancelFriendRequestDto {
    private String senderEmail;
    private UUID requestId;
}
