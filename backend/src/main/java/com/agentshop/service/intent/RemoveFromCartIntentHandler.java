package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.model.CartItem;
import com.agentshop.service.CartService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RemoveFromCartIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;
    private final CartService cartService;

    public RemoveFromCartIntentHandler(ShoppingTools shoppingTools, CartService cartService) {
        this.shoppingTools = shoppingTools;
        this.cartService = cartService;
    }

    @Override
    public boolean canHandle(String message) {
        return message.contains("remove") || message.contains("delete from cart") || 
               message.contains("clear cart");
    }

    @Override
    public int priority() {
        return 80;
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);
        
        String query = message.toLowerCase()
                .replace("remove", "").replace("delete from cart", "")
                .replace("clear cart", "").trim();
                
        // 1. Try strict ID matching first (e.g., "product 1", "product #1", "id 1")
        Matcher m = Pattern.compile("(?i)(?:product\\s*#?|id\\s*)(\\d+)").matcher(message);
        if (m.find()) {
            Long pid = Long.parseLong(m.group(1));
            return shoppingTools.removeFromCart(pid);
        } 
        
        // 2. Try matching by name
        if (!query.isEmpty()) {
            List<CartItem> cart = cartService.getCart(sessionId);
            for (CartItem item : cart) {
                String pName = item.getProduct().getName().toLowerCase();
                
                // Exact or substring match
                if (pName.contains(query) || query.contains(pName)) {
                    return shoppingTools.removeFromCart(item.getProduct().getId());
                }
                
                // Token-based match (e.g., "iphone 16")
                String[] tokens = query.split("\\s+");
                boolean allMatch = true;
                for (String t : tokens) {
                    if (t.length() > 2 && !pName.contains(t)) {
                        allMatch = false;
                        break;
                    }
                }
                if (allMatch && tokens.length > 0) {
                    return shoppingTools.removeFromCart(item.getProduct().getId());
                }
            }
        }
        
        return "Please specify the exact product ID or name to remove (e.g., 'remove product 1' or 'remove iphone').";
    }
}
