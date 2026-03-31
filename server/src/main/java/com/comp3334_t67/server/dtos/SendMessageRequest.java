package com.comp3334_t67.server.dtos;

import lombok.Data;

@Data
public class SendMessageRequest {
    private String senderEmail;
    private String message_hash;
    private String nouce;

}
