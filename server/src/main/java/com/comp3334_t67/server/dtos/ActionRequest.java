package com.comp3334_t67.server.dtos;

import com.comp3334_t67.server.enums.FriendRequestStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionRequest {
    
    private FriendRequestStatus action;

    public boolean isAccepted() {
        return action == FriendRequestStatus.ACCEPTED;
    }
}
