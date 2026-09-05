package com.agentshop.service.agent;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.repository.ProductRepository;
import com.agentshop.service.intent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sales Agent — Specialized agent for product discovery, search, upsell/cross-sell,
 * coupon suggestions, and category browsing.
 *
 * Implements SELF-CORRECTING SEARCH: if a search returns 0 results, it autonomously
 * broadens the query and suggests the cheapest alternative instead of a dead-end.
 */
@Service
public class SalesAgent {

    private static final Logger log = LoggerFactory.getLogger(SalesAgent.class);

    private final ShoppingTools shoppingTools;
    private final ProductRepository productRepository;
    private final CouponIntentHandler couponHandler;
    private final CategoriesIntentHandler categoriesHandler;

    public SalesAgent(ShoppingTools shoppingTools,
                      ProductRepository productRepository,
                      CouponIntentHandler couponHandler,
                      CategoriesIntentHandler categoriesHandler) {
        this.shoppingTools = shoppingTools;
        this.productRepository = productRepository;
        this.couponHandler = couponHandler;
        this.categoriesHandler = categoriesHandler;
    }

    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);
        String lowerMsg = message.toLowerCase().trim();

        // Route to coupon handler
        if (couponHandler.canHandle(lowerMsg)) {
            return couponHandler.handle(sessionId, lowerMsg);
        }

        // Route to categories handler
        if (categoriesHandler.canHandle(lowerMsg)) {
            return categoriesHandler.handle(sessionId, lowerMsg);
        }

        // Product details request
        Pattern detailsPattern = Pattern.compile("(?:details|info|about|describe|specs)\\s+(?:of\\s+)?(?:product\\s+)?(?:#)?(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher detailsMatcher = detailsPattern.matcher(message);
        if (detailsMatcher.find()) {
            try {
                Long productId = Long.parseLong(detailsMatcher.group(1));
                return shoppingTools.getProductDetails(productId);
            } catch (NumberFormatException e) {
                // fall through to search
            }
        }

        // Self-correcting product search
        return selfCorrectingSearch(sessionId, message);
    }

    /**
     * Self-Correcting Search Pipeline:
     * 1. Search with full query + price constraint
     * 2. If 0 results → drop price constraint
     * 3. If still 0 → search by detected category only
     * 4. If still 0 → find cheapest product in category and suggest it
     * 5. If nothing works → suggest browsing categories
     */
    private String selfCorrectingSearch(String sessionId, String message) {
        String cleanMessage = message.replaceAll("(?<=\\d),(?=\\d)", "");

        // Extract price constraint
        Double maxPrice = null;
        Matcher priceMatcher = Pattern.compile("(?i)(?:under|below|less than|budget|max|within|<=|<)?\\s*(?:₹|rs\\.?|inr)?\\s*(\\d{3,7})").matcher(cleanMessage);
        if (priceMatcher.find()) {
            try {
                maxPrice = Double.parseDouble(priceMatcher.group(1));
            } catch (Exception ignored) {}
        }

        // Detect category
        String detectedCategory = detectCategory(message);

        // Extract search query
        String query = cleanMessage
                .replaceAll("(?i)\\b(i|am|looking|for|show|me|find|search|recommend|suggest|give|want|to|buy|purchase|need|can|you|please|some|the|a|an|under|below|less|than|within|budget|rs|inr|items|products|with|top|best|good|cheap|cheapest|affordable)\\b", " ")
                .replaceAll("₹|\\$|\\d{3,7}", " ")
                .replaceAll("[^a-zA-Z0-9 ]", "")
                .replaceAll("\\s+", " ")
                .trim();

        // Step 1: Full search
        String result = shoppingTools.searchProducts(query, detectedCategory, maxPrice);
        if (!isEmptyResult(result)) {
            return result + "\n\n💡 _Say **'Add [Product ID]'** to add to cart, or ask me for details!_";
        }

        // Step 2: Drop price constraint
        if (maxPrice != null) {
            log.info("🔄 Self-correction: dropping price constraint for '{}'", query);
            result = shoppingTools.searchProducts(query, detectedCategory, null);
            if (!isEmptyResult(result)) {
                String priceNote = String.format("\n\n⚠️ _I couldn't find items under ₹%.0f, but here are the best matches:_\n", maxPrice);
                return priceNote + result + "\n💡 _Say **'Add [Product ID]'** to add to cart!_";
            }
        }

        // Step 3: Category-only search
        if (detectedCategory != null) {
            log.info("🔄 Self-correction: searching category '{}' only", detectedCategory);
            result = shoppingTools.searchProducts("", detectedCategory, maxPrice);
            if (!isEmptyResult(result)) {
                return "🔍 I broadened the search to show you popular **" + detectedCategory + "**:\n\n" + result +
                       "\n💡 _Say **'Add [Product ID]'** to add to cart!_";
            }
        }

        // Step 4: Find cheapest in category
        if (detectedCategory != null) {
            log.info("🔄 Self-correction: finding cheapest in category '{}'", detectedCategory);
            var cheapest = productRepository.findCheapestInCategory(detectedCategory);
            if (cheapest != null) {
                String suggestion = String.format(
                    "🤔 I couldn't find an exact match, but the **most affordable %s** is:\n\n" +
                    "✨ **%s** — ₹%s (⭐%.1f, %d reviews)\n\n" +
                    "💡 _Say **'Add %d'** to add it to your cart, or try a different search!_",
                    detectedCategory, cheapest.getName(), cheapest.getPrice().toPlainString(),
                    cheapest.getRating(), cheapest.getReviewCount(), cheapest.getId()
                );
                return suggestion;
            }
        }

        // Step 5: Ultimate fallback
        return "😅 I couldn't find products matching your query. Try:\n\n" +
               "  • **Browse categories** — say 'show categories'\n" +
               "  • **Search by type** — e.g. 'show laptops', 'earbuds under 3000'\n" +
               "  • **Search by brand** — e.g. 'Apple phones', 'Sony headphones'\n\n" +
               "💡 _I'm here to help you find exactly what you need!_";
    }

    private String detectCategory(String message) {
        String lowerMsg = " " + message.toLowerCase().replaceAll("[^a-z0-9 ]", " ") + " ";
        if (lowerMsg.matches(".*\\b(phone|mobile|iphone|samsung|smartphone)\\b.*")) return "Smartphones";
        if (lowerMsg.matches(".*\\b(earbud|headphone|audio|earphone|airpod)\\b.*")) return "Headphones";
        if (lowerMsg.matches(".*\\b(watch|smartwatch)\\b.*")) return "Smartwatches";
        if (lowerMsg.matches(".*\\b(ipad|tablet|tab)\\b.*")) return "Tablets";
        if (lowerMsg.matches(".*\\b(laptop|macbook|computer|pc)\\b.*")) return "Laptops";
        if (lowerMsg.matches(".*\\b(camera|dslr|lens)\\b.*")) return "Cameras";
        return null;
    }

    private boolean isEmptyResult(String result) {
        return result == null || result.contains("No products found") || result.contains("Found 0 products");
    }
}
