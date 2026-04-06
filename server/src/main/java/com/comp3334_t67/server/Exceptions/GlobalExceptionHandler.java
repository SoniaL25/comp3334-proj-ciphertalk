package com.comp3334_t67.server.Exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.comp3334_t67.server.dtos.ApiResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({
        InvalidCredentialsException.class,
        OtpInvalidException.class,
        MessageValidationException.class,
        InvalidInputException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(RuntimeException ex) {
        log.warn("Bad request handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
        OtpSessionMissingException.class,
        FriendRequestOwnershipException.class,
        ChatMembershipException.class,
        MessagingBlockedException.class,
        UsersNotFriendsException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleForbidden(RuntimeException ex) {
        log.warn("Forbidden request handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
        UserNotFoundException.class,
        FriendRequestNotFoundException.class,
        ChatNotFoundException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleNotFound(RuntimeException ex) {
        log.warn("Not found handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler({
        UserAlreadyExistsException.class,
        FriendRequestStateException.class,
        SelfBlockNotAllowedException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleConflict(RuntimeException ex) {
        log.warn("Conflict handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleRateLimit(RateLimitExceededException ex) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailDelivery(EmailDeliveryException ex) {
        log.error("Email delivery failure handled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneric(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error("Internal server error"));
    }
    
}
