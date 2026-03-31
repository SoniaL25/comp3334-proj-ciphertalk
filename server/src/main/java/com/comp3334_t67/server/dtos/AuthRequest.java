package com.comp3334_t67.server.dtos;

import lombok.Data;

@Data
public class AuthRequest {
    
    private String email;
    private String password_hash;
    
}
