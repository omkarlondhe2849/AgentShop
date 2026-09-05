package com.agentshop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(length = 5000)
    private String content;

    @Column(length = 2000)
    private String toolCalls; // JSON array of tool invocations

    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public enum Role {
        USER, ASSISTANT, SYSTEM
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Role getRole() {
        return this.role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return this.content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getToolCalls() {
        return this.toolCalls;
    }

    public void setToolCalls(String toolCalls) {
        this.toolCalls = toolCalls;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Conversation() {}

    public Conversation(Long id, String sessionId, Role role, String content, String toolCalls, LocalDateTime timestamp) {
        this.id = id;
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.toolCalls = toolCalls;
        this.timestamp = timestamp;
    }

    public static ConversationBuilder builder() {
        return new ConversationBuilder();
    }

    public static class ConversationBuilder {
        private Long id;
        private String sessionId;
        private Role role;
        private String content;
        private String toolCalls;
        private LocalDateTime timestamp;

        public ConversationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ConversationBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public ConversationBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public ConversationBuilder content(String content) {
            this.content = content;
            return this;
        }

        public ConversationBuilder toolCalls(String toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public ConversationBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Conversation build() {
            return new Conversation(this.id, this.sessionId, this.role, this.content, this.toolCalls, this.timestamp);
        }
    }
}
