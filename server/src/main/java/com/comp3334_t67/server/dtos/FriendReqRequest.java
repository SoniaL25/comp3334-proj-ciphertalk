package com.comp3334_t67.server.dtos;

import lombok.*;

@Data
public class FriendReqRequest {
    
    private String senderEmail;
    private String receiverEmail;
}
