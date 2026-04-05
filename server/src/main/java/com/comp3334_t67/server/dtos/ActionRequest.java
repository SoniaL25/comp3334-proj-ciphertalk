package com.comp3334_t67.server.dtos;

import com.comp3334_t67.server.enums.FriendRequestStatus;

public class ActionRequest {
    
    private FriendRequestStatus action;

    public boolean isAccepted() {
        return action == FriendRequestStatus.ACCEPTED;
    }

    public void setAccepted(boolean accepted) {
        this.action = accepted ? FriendRequestStatus.ACCEPTED : FriendRequestStatus.REJECTED;
    }
}
