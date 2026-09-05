package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import org.springframework.stereotype.Component;

@Component
public class ViewCartIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;

    public ViewCartIntentHandler(ShoppingTools shoppingTools) {
        this.shoppingTools = shoppingTools;
    }

    @Override
    public boolean canHandle(String message) {
        return message.equals("cart") || message.contains("view cart") || 
               message.contains("show cart") || message.contains("what's in my cart") || 
               message.contains("my cart") || message.contains("cart summary");
    }

    @Override
    public int priority() {
        return 70;
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);
        return shoppingTools.viewCart();
    }
}
