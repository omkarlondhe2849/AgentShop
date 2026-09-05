package com.agentshop.service.intent;

import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class IntentRouter {

    private final List<IntentHandler> handlers;
    private final SearchIntentHandler searchIntentHandler;

    public IntentRouter(List<IntentHandler> handlers, SearchIntentHandler searchIntentHandler) {
        // Sort handlers by priority descending
        this.handlers = handlers.stream()
                .filter(h -> !(h instanceof SearchIntentHandler))
                .sorted(Comparator.comparingInt(IntentHandler::priority).reversed())
                .toList();
        this.searchIntentHandler = searchIntentHandler;
    }

    public String route(String sessionId, String message) {
        String lowerMsg = message.toLowerCase().trim();
        
        return handlers.stream()
                .filter(h -> h.canHandle(lowerMsg))
                .findFirst()
                .map(h -> h.handle(sessionId, lowerMsg))
                .orElseGet(() -> searchIntentHandler.handle(sessionId, lowerMsg));
    }
}
