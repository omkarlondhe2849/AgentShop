package com.agentshop.config;

import com.agentshop.agent.ShoppingTools;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.Map;
import java.util.function.Function;

/**
 * Spring AI configuration — registers ShoppingTools methods as callable functions for the LLM.
 */
@Configuration
public class AgentConfig {

    private final ShoppingTools shoppingTools;

    public AgentConfig(ShoppingTools shoppingTools) {
        this.shoppingTools = shoppingTools;
    }

    // Register tool functions as Spring Beans so Spring AI can discover them

    @Bean
    @Description("Search products in the catalog by query text, optional category, and optional maximum price")
    public Function<SearchRequest, String> searchProducts() {
        return request -> shoppingTools.searchProducts(
                request.query(), request.category(), request.maxPrice());
    }

    @Bean
    @Description("Get detailed information about a specific product by its ID")
    public Function<ProductIdRequest, String> getProductDetails() {
        return request -> shoppingTools.getProductDetails(request.productId());
    }

    @Bean
    @Description("Add a product to the shopping cart by product ID and quantity")
    public Function<AddToCartRequest, String> addToCart() {
        return request -> shoppingTools.addToCart(request.productId(), request.quantity());
    }

    @Bean
    @Description("Remove a product from the shopping cart by product ID")
    public Function<ProductIdRequest, String> removeFromCart() {
        return request -> shoppingTools.removeFromCart(request.productId());
    }

    @Bean
    @Description("View the current contents of the shopping cart")
    public Function<EmptyRequest, String> viewCart() {
        return request -> shoppingTools.viewCart();
    }

    @Bean
    @Description("Initiate checkout and create a Razorpay payment link. Requires customer name, email, phone number, and delivery address. IMPORTANT: Always explicitly ask the user for their delivery address before calling this.")
    public Function<CheckoutRequest, String> initiateCheckout() {
        return request -> shoppingTools.initiateCheckout(
                request.customerName(), request.customerEmail(), request.customerPhone(), request.deliveryAddress());
    }

    @Bean
    @Description("Get upsell and cross-sell product recommendations based on a product ID")
    public Function<ProductIdRequest, String> getUpsellRecommendations() {
        return request -> shoppingTools.getUpsellRecommendations(request.productId());
    }

    @Bean
    @Description("List all available product categories in the store")
    public Function<EmptyRequest, String> listCategories() {
        return request -> shoppingTools.listCategories();
    }

    @Bean
    @Description("Check the payment and order status for a given order ID")
    public Function<OrderIdRequest, String> checkPaymentStatus() {
        return request -> shoppingTools.checkPaymentStatus(request.orderId());
    }

    // ---- Request records for function calling ----

    public record SearchRequest(String query, String category, Double maxPrice) {}
    public record ProductIdRequest(Long productId) {}
    public record AddToCartRequest(Long productId, Integer quantity) {}
    public record CheckoutRequest(String customerName, String customerEmail, String customerPhone, String deliveryAddress) {}
    public record OrderIdRequest(Long orderId) {}
    public record EmptyRequest() {}
}
