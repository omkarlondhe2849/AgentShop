package com.agentshop.event;

import com.agentshop.model.CartItem;
import com.agentshop.model.Order;
import com.agentshop.model.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ShoppingEvents {

    record ProductSearchedEvent(String sessionId, String query, String category, Double maxPrice, List<Product> results) {}
    
    record ProductViewedEvent(String sessionId, Product product) {}
    
    record CartItemAddedEvent(String sessionId, Product product, int quantity, CartItem cartItem, BigDecimal newCartTotal) {}
    
    record CartAddFailedEvent(String sessionId, Product product, int quantity, String reason) {}
    
    record CartItemRemovedEvent(String sessionId, Product product) {}
    
    record CartViewedEvent(String sessionId, BigDecimal cartTotal) {}
    
    record CheckoutInitiatedEvent(String sessionId, Order order, int itemCount) {}
    
    record CheckoutBlockedEvent(String sessionId, BigDecimal cartTotal, long maxAmountPaise) {}
    
    record PaymentLinkCreatedEvent(String sessionId, Order order, String razorpayOrderId, String paymentLinkId, String shortUrl) {}
    
    record PaymentLinkFailedEvent(String sessionId, Order order, String errorMessage) {}
    
    record UpsellOfferedEvent(String sessionId, Product baseProduct, List<Product> suggestions) {}
    
    record CategoriesListedEvent(String sessionId, List<String> categories) {}
}
