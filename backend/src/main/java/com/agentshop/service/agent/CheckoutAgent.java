package com.agentshop.service.agent;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.service.intent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Checkout Agent — Specialized agent for cart management, checkout flow,
 * and payment status tracking.
 *
 * Handles: add to cart, remove from cart, view cart, checkout, payment status.
 * Strictly enforces order boundaries and Razorpay payment flow.
 */
@Service
public class CheckoutAgent {

    private static final Logger log = LoggerFactory.getLogger(CheckoutAgent.class);

    private final AddToCartIntentHandler addToCartHandler;
    private final RemoveFromCartIntentHandler removeFromCartHandler;
    private final ViewCartIntentHandler viewCartHandler;
    private final CheckoutIntentHandler checkoutHandler;
    private final ShoppingTools shoppingTools;

    public CheckoutAgent(AddToCartIntentHandler addToCartHandler,
                         RemoveFromCartIntentHandler removeFromCartHandler,
                         ViewCartIntentHandler viewCartHandler,
                         CheckoutIntentHandler checkoutHandler,
                         ShoppingTools shoppingTools) {
        this.addToCartHandler = addToCartHandler;
        this.removeFromCartHandler = removeFromCartHandler;
        this.viewCartHandler = viewCartHandler;
        this.checkoutHandler = checkoutHandler;
        this.shoppingTools = shoppingTools;
    }

    /**
     * Check if this agent can handle the given message.
     */
    public boolean canHandle(String message) {
        return addToCartHandler.canHandle(message) ||
               removeFromCartHandler.canHandle(message) ||
               viewCartHandler.canHandle(message) ||
               checkoutHandler.canHandle(message) ||
               message.matches(".*(?:payment|order)\\s*(?:status|check|track).*") ||
               message.matches(".*check\\s*(?:payment|order).*");
    }

    /**
     * Handle a checkout-related request.
     */
    public String handle(String sessionId, String message) {
        log.info("🛒 Checkout Agent handling: '{}'", message);
        shoppingTools.setCurrentSessionId(sessionId);
        String lowerMsg = message.toLowerCase().trim();

        // Payment status check
        if (lowerMsg.matches(".*(?:payment|order)\\s*(?:status|check|track).*") ||
            lowerMsg.matches(".*check\\s*(?:payment|order).*")) {
            // Try to extract order ID
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?:order|#)\\s*(\\d+)").matcher(message);
            if (m.find()) {
                try {
                    Long orderId = Long.parseLong(m.group(1));
                    return "🛒 **Checkout Agent**\n\n" + shoppingTools.checkPaymentStatus(orderId);
                } catch (NumberFormatException e) {
                    // fall through
                }
            }
            return "🛒 **Checkout Agent**\n\nPlease provide your Order ID to check the status. Example: 'Check order #42'";
        }

        // Route to specific handlers
        if (addToCartHandler.canHandle(lowerMsg)) {
            return "🛒 **Checkout Agent**\n\n" + addToCartHandler.handle(sessionId, lowerMsg);
        }
        if (removeFromCartHandler.canHandle(lowerMsg)) {
            return "🛒 **Checkout Agent**\n\n" + removeFromCartHandler.handle(sessionId, lowerMsg);
        }
        if (viewCartHandler.canHandle(lowerMsg)) {
            return "🛒 **Checkout Agent**\n\n" + viewCartHandler.handle(sessionId, lowerMsg);
        }
        if (checkoutHandler.canHandle(lowerMsg)) {
            return checkoutHandler.handle(sessionId, lowerMsg);
        }

        return "🛒 **Checkout Agent**\n\nI can help you with:\n" +
               "  • **Add to cart** — 'add 42' or 'add product 42'\n" +
               "  • **View cart** — 'show my cart'\n" +
               "  • **Remove item** — 'remove 42'\n" +
               "  • **Checkout** — 'proceed to checkout'\n" +
               "  • **Payment status** — 'check order #1'";
    }
}
