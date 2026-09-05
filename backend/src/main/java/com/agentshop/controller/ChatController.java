package com.agentshop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agentshop.dto.ChatRequest;
import com.agentshop.dto.ChatResponse;
import com.agentshop.model.Conversation;
import com.agentshop.service.ChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);


    private final ChatService chatService;

    @PostMapping("/message")
    public ResponseEntity<ChatResponse> sendMessage(@RequestBody ChatRequest request) {
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = UUID.randomUUID().toString();
        }

        log.info("Chat request from session: {}", sessionId);

        String response = chatService.chat(sessionId, request.getMessage());

        ChatResponse chatResponse = ChatResponse.builder()
                .sessionId(sessionId)
                .message(response)
                .role("assistant")
                .timestamp(System.currentTimeMillis())
                .build();

        return ResponseEntity.ok(chatResponse);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<Conversation>> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(chatService.getConversationHistory(sessionId));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<String>> getSessions() {
        return ResponseEntity.ok(chatService.getAllSessions());
    }

    @PostMapping("/new-session")
    public ResponseEntity<Map<String, String>> createSession() {
        String sessionId = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }
}
