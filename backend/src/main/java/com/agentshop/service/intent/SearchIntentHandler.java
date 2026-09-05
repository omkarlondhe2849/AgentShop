package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.repository.ProductRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;
    private final ProductRepository productRepository;

    public SearchIntentHandler(ShoppingTools shoppingTools, ProductRepository productRepository) {
        this.shoppingTools = shoppingTools;
        this.productRepository = productRepository;
    }

    @Override
    public boolean canHandle(String message) {
        return true; // Fallback handler
    }

    @Override
    public int priority() {
        return 0; // Lowest priority
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);

        // Remove commas from numbers to easily extract prices (e.g. 3,000 -> 3000)
        String cleanMessage = message.replaceAll("(?<=\\d),(?=\\d)", "");

        Double maxPrice = null;
        Matcher priceMatcher = Pattern.compile("(?i)(?:under|below|less than|budget|max|within|<=|<)?\\s*(?:₹|rs\\.?|inr)?\\s*(\\d{3,7})").matcher(cleanMessage);
        if (priceMatcher.find()) {
            try {
                maxPrice = Double.parseDouble(priceMatcher.group(1));
            } catch (Exception ignored) {}
        }

        String detectedCategory = null;
        List<String> categories = productRepository.findAllCategories();
        for (String cat : categories) {
            if (message.toLowerCase().contains(cat.toLowerCase())) {
                detectedCategory = cat;
                break;
            }
        }
        if (detectedCategory == null) {
            String lowerMsg = message.toLowerCase();
            if (lowerMsg.contains("phone") || lowerMsg.contains("mobile")) {
                detectedCategory = "Smartphones";
            } else if (lowerMsg.contains("earbud") || lowerMsg.contains("headphone") || lowerMsg.contains("audio") || lowerMsg.contains("earphone")) {
                detectedCategory = "Headphones";
            } else if (lowerMsg.contains("watch") || lowerMsg.contains("smartwatch")) {
                detectedCategory = "Smartwatches";
            } else if (lowerMsg.contains("ipad") || lowerMsg.contains("tablet") || lowerMsg.contains("tab")) {
                detectedCategory = "Tablets";
            } else if (lowerMsg.contains("laptop") || lowerMsg.contains("macbook") || lowerMsg.contains("computer")) {
                detectedCategory = "Laptops";
            } else if (lowerMsg.contains("camera") || lowerMsg.contains("dslr")) {
                detectedCategory = "Cameras";
            }
        }

        // Extract key search terms
        String query = cleanMessage
                .replaceAll("(?i)\\b(i|am|looking|for|show|me|find|search|recommend|suggest|give|want|to|buy|purchase|need|can|you|please|some|the|a|an|under|below|less|than|within|budget|rs|inr|items|products|with|top)\\b", " ")
                .replaceAll("₹|\\$|\\d{3,7}", " ") // Remove the price number itself from the query string
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();

        String searchResult = shoppingTools.searchProducts(query, detectedCategory, maxPrice);

        // Fallback 1: Broaden search by dropping price constraint
        if ((searchResult.contains("No products found") || searchResult.contains("Found 0 products")) && maxPrice != null) {
            searchResult = shoppingTools.searchProducts(query, detectedCategory, null);
        }

        // Fallback 2: Broaden search by dropping query entirely and showing category items
        if (searchResult.contains("No products found") || searchResult.contains("Found 0 products")) {
            if (detectedCategory != null || maxPrice != null) {
                searchResult = shoppingTools.searchProducts("", detectedCategory, maxPrice);
            }
        }

        return searchResult + "\n\n💡 _Say **'Add [Product ID]'** (e.g. 'Add 2') to add any item to your cart, or **'Checkout'** when ready!_";
    }
}
