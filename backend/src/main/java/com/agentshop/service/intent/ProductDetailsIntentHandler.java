package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProductDetailsIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;

    public ProductDetailsIntentHandler(ShoppingTools shoppingTools) {
        this.shoppingTools = shoppingTools;
    }

    @Override
    public boolean canHandle(String message) {
        return message.contains("details") || message.contains("tell me more about") || 
               message.contains("describe");
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);
        
        Matcher m = Pattern.compile("(\\d+)").matcher(message);
        if (m.find()) {
            Long pid = Long.parseLong(m.group(1));
            return shoppingTools.getProductDetails(pid);
        }
        return "Could you specify which product ID you want details for?";
    }
}
