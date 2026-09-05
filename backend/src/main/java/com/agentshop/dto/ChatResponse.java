package com.agentshop.dto;


public class ChatResponse {
    private String sessionId;
    private String message;
    private String role;
    private long timestamp;

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRole() {
        return this.role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public ChatResponse() {}

    public ChatResponse(String sessionId, String message, String role, long timestamp) {
        this.sessionId = sessionId;
        this.message = message;
        this.role = role;
        this.timestamp = timestamp;
    }

    public static ChatResponseBuilder builder() {
        return new ChatResponseBuilder();
    }

    public static class ChatResponseBuilder {
        private String sessionId;
        private String message;
        private String role;
        private long timestamp;

        public ChatResponseBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public ChatResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ChatResponseBuilder role(String role) {
            this.role = role;
            return this;
        }

        public ChatResponseBuilder timestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(this.sessionId, this.message, this.role, this.timestamp);
        }
    }
}
