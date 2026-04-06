package com.comp3334_t67.server.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SendMessageRequest {
    private String senderEmail;

    @JsonProperty("content")
    private String content;

    @JsonProperty("nonce")
    private String nonce;

    private String clientMessageId;
    private String tag;

}
