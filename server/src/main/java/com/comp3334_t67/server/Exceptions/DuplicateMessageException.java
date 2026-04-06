package com.comp3334_t67.server.Exceptions;

public class DuplicateMessageException extends RuntimeException {
    public DuplicateMessageException(String message) {
        super(message);
    }
    
}