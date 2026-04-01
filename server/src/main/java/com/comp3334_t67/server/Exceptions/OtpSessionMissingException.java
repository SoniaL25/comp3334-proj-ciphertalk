package com.comp3334_t67.server.Exceptions;

public class OtpSessionMissingException extends RuntimeException {
    public OtpSessionMissingException(String message) {
        super(message);
    }
}