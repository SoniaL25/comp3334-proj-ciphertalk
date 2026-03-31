package com.comp3334_t67.server.controllers;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import lombok.AllArgsConstructor;

import com.comp3334_t67.server.services.*;

import jakarta.servlet.http.HttpSession;

import com.comp3334_t67.server.dtos.*;
import com.comp3334_t67.server.models.*;

import java.util.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/chats")
public class MessageController {

    private final MessageService messageService;

    // send message
    @PostMapping("/{chatId}")
    public ResponseEntity<ApiResponse<Void>> sendMessage(@PathVariable String chatId, @RequestBody SendMessageRequest request) {
        messageService.sendMessage(chatId, request.getSenderEmail(), request.getMessage_hash(), request.getNouce());

        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", null));
    }

    // receiver gets messages TODO: message dto
    @GetMapping("/{chatId}")
    public ResponseEntity<ApiResponse<List<Message>>> getMessages(@PathVariable String chatId, HttpSession session) {
        String email = (String) session.getAttribute("OTP_USER");
        List<Message> messages = messageService.getUnreadMessagesForReceiver(email,chatId);
        return ResponseEntity.ok(ApiResponse.success("Messages retrieved successfully", messages));
    }


    
}
