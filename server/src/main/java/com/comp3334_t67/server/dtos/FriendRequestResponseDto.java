package com.comp3334_t67.server.dtos;

import lombok.Data;
import java.util.UUID;

@Data
public class FriendRequestResponseDto {
    private UUID requestId;
    private boolean accepted;

    public boolean isAccepted() {
        return accepted;
    }
}
