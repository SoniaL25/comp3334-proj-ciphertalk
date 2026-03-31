package com.comp3334_t67.server.dtos;

import lombok.Data;

@Data
public class OtpVerificationRequest {
    String email;
    int otp;
}
