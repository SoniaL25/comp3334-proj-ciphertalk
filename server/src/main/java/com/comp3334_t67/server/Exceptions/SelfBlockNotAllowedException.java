package com.comp3334_t67.server.Exceptions;

public class SelfBlockNotAllowedException extends RuntimeException {
    public SelfBlockNotAllowedException(String message) {
        super(message);
    }
}