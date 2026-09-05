package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.model.Product;
import com.agentshop.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AddToCartIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;
    private final ProductRepository productRepository;

    public AddToCartIntentHandler(ShoppingTools shoppingTools, ProductRepository productRepository) {
        this.shoppingTools = shoppingTools;
        this.productRepository = productRepository;
    }

    @Override
    public boolean canHandle(String message) {
        return message.contains("add") || message.startsWith("buy ") || 
               message.contains("put in cart") || message.contains("include ");
    }

    @Override
    public int priority() {
        return 90;
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);
        
        Matcher idMatcher = Pattern.compile("(?:product|item|id|#)?\\s*(\\d+)").matcher(message);
        Long matchedId = null;
        if (idMatcher.find()) {
            matchedId = Long.parseLong(idMatcher.group(1));
        }

        int quantity = 1;
        Matcher qtyMatcher = Pattern.compile("(?:qty|quantity|x|count)\\s*[:=]?\\s*(\\d+)").matcher(message);
        if (qtyMatcher.find()) {
            quantity = Integer.parseInt(qtyMatcher.group(1));
        }

        if (matchedId != null && productRepository.existsById(matchedId)) {
            return shoppingTools.addToCart(matchedId, quantity);
        }

        List<Product> allProducts = productRepository.findAll();
        for (Product p : allProducts) {
            if (message.contains(p.getName().toLowerCase()) || 
                p.getName().toLowerCase().contains(message.replace("add", "").trim())) {
                return shoppingTools.addToCart(p.getId(), quantity);
            }
        }
        
        return "I couldn't identify the product. Could you please specify the Product ID? (e.g. 'Add product 1')";
    }
}
