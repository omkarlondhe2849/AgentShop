package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import org.springframework.stereotype.Component;

@Component
public class CategoriesIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;

    public CategoriesIntentHandler(ShoppingTools shoppingTools) {
        this.shoppingTools = shoppingTools;
    }

    @Override
    public boolean canHandle(String message) {
        return message.contains("categories") || message.contains("what do you sell") || 
               message.contains("what categories");
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);
        return shoppingTools.listCategories();
    }
}
