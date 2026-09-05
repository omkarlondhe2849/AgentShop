package com.agentshop.controller;

import com.agentshop.model.AuditLog;
import com.agentshop.model.Conversation;
import com.agentshop.service.AuditService;
import com.agentshop.service.ChatService;
import com.agentshop.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public DashboardController(OrderService orderService, AuditService auditService, ChatService chatService) {
        this.orderService = orderService;
        this.auditService = auditService;
        this.chatService = chatService;
    }


    private final OrderService orderService;
    private final AuditService auditService;
    private final ChatService chatService;

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(@RequestParam(required = false, defaultValue = "all") String range) {
        return ResponseEntity.ok(orderService.getDashboardStats(range));
    }

    @GetMapping("/audit-trail")
    public ResponseEntity<List<AuditLog>> getAuditTrail(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false, defaultValue = "all") String range) {
        return ResponseEntity.ok(auditService.getAuditTrail(sessionId, range));
    }

    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations() {
        List<String> sessions = chatService.getAllSessions();
        List<Map<String, Object>> convos = new ArrayList<>();

        for (String sessionId : sessions) {
            List<Conversation> history = chatService.getConversationHistory(sessionId);
            Map<String, Object> convo = new HashMap<>();
            convo.put("sessionId", sessionId);
            convo.put("messageCount", history.size());
            convo.put("lastMessage", history.isEmpty() ? null : history.get(history.size() - 1).getContent());
            convo.put("startedAt", history.isEmpty() ? null : history.get(0).getTimestamp());
            convos.add(convo);
        }

        return ResponseEntity.ok(Map.of(
                "totalConversations", sessions.size(),
                "conversations", convos
        ));
    }
}
