package com.agentshop.service.intent;

public interface IntentHandler {
    boolean canHandle(String message);
    int priority(); // Higher priority handled first
    String handle(String sessionId, String message);
}
