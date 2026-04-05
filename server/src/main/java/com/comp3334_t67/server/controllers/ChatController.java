package com.comp3334_t67.server.controllers;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;

import com.comp3334_t67.server.services.*;

import jakarta.servlet.http.HttpSession;

import com.comp3334_t67.server.dtos.*;

import java.util.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {

    private final MessageService messageService;
    private final ChatService chatService;

    // get all friend chats for user in session
    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendChatDto>>> getFriendChats(HttpSession session) {
        String email = requireSessionEmail(session);
        List<FriendChatDto> chats = chatService.getFriendChats(email);
        return ResponseEntity.ok(ApiResponse.success("Friend chats retrieved successfully", chats));
    }

    // send message
    @PostMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Void>> sendMessage(@PathVariable String chatId, @RequestBody SendMessageRequest request, HttpSession session) {
        String senderEmail = requireSessionEmail(session);
        chatService.sendMessage(chatId, senderEmail, request.getContent(), request.getNonce());

        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", null));
    }

    // receiver gets messages TODO: message dto
    @GetMapping("/{chatId}")
    public ResponseEntity<ApiResponse<List<MessageDto>>> getMessages(@PathVariable String chatId, HttpSession session) {
        String email = requireSessionEmail(session);
        List<MessageDto> messages = messageService.getUnreadMessagesForReceiver(email,chatId);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }

    // remove friend
    @DeleteMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Void>> removeFriend(@PathVariable String chatId, HttpSession session) {
        String email = requireSessionEmail(session);
        chatService.removeFriend(chatId, email);
        return ResponseEntity.ok(ApiResponse.success("Friend removed successfully", null));
    }

    // HELPER METHOD

    // Require that the session contains a valid authenticated user email, otherwise throw an exception
    private String requireSessionEmail(HttpSession session) {
        if (session == null) {
            throw new IllegalStateException("No active session");
        }

        String email = (String) session.getAttribute("OTP_USER");
        if (email == null || email.isBlank()) {
            throw new IllegalStateException("No authenticated user in session");
        }

        return email;
    }


    
}
