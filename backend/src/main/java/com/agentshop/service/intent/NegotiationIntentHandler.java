package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.model.CartItem;
import com.agentshop.service.CartService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NegotiationIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;
    private final CartService cartService;

    public NegotiationIntentHandler(ShoppingTools shoppingTools, CartService cartService) {
        this.shoppingTools = shoppingTools;
        this.cartService = cartService;
    }

    @Override
    public boolean canHandle(String message) {
        return message.contains("discount") || message.contains("offer") || 
               message.contains("cheaper") || message.contains("price match") ||
               message.contains("reduce") || message.contains("coupon") ||
               message.contains("lower the price");
    }

    @Override
    public int priority() {
        return 95; // High priority, runs before fallback search and after checkout
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);
        List<CartItem> cart = cartService.getCart(sessionId);
        
        if (cart.isEmpty()) {
            return "I'd love to offer you a discount! Please add some items to your cart first, and we can discuss the best price. 🤝";
        }

        int totalItems = cart.stream().mapToInt(CartItem::getQuantity).sum();
        
        if (totalItems >= 3) {
            return "Since you're buying " + totalItems + " items, I can apply a **10% bulk discount** to your order during checkout! 🎁 Just say 'Checkout' to proceed with the discount.";
        } else if (totalItems == 2) {
            return "If you add one more item to your cart, I can unlock a **10% bulk discount** for your entire order! 🛍️ What else are you looking for?";
        } else {
            return "Our current prices are very competitive, but if you buy 3 or more items in total, I can give you a **special 10% discount**! Let me know if you want me to recommend some items.";
        }
    }
}
