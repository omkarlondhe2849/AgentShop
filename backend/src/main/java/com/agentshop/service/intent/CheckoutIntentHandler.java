package com.agentshop.service.intent;

import com.agentshop.service.CartService;
import org.springframework.stereotype.Component;

@Component
public class CheckoutIntentHandler implements IntentHandler {

    private final CartService cartService;

    public CheckoutIntentHandler(CartService cartService) {
        this.cartService = cartService;
    }

    @Override
    public boolean canHandle(String message) {
        return message.contains("checkout") || message.contains("chekout") || 
               message.contains("check out") || message.contains("pay") || 
               message.contains("buy now") || message.contains("place order") || 
               message.contains("complete order") || message.contains("purchase");
    }

    @Override
    public int priority() {
        return 100;
    }

    @Override
    public String handle(String sessionId, String message) {
        if (cartService.getCart(sessionId).isEmpty()) {
            return "🛒 Your cart is currently empty! Please search for items or ask me for recommendations first before checking out.";
        }
        return "__ACTION_REQUEST_ADDRESS__";
    }
}
