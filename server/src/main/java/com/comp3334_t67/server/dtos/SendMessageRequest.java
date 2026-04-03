package com.comp3334_t67.server.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SendMessageRequest {
    private String senderEmail;

    @JsonProperty("message_hash")
    private String messageHash;

    @JsonProperty("nouce")
    private String nonce;

}
