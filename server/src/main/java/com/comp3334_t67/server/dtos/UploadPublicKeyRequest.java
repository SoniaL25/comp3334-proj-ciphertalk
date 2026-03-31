package com.comp3334_t67.server.dtos;

import lombok.Data;

@Data
public class UploadPublicKeyRequest {
    
    private String email;
    private String publicKey;
}
