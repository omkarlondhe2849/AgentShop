package com.agentshop.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agentshop.model.CartItem;
import com.agentshop.model.Order;
import com.agentshop.model.Product;
import com.agentshop.repository.ProductRepository;
import com.agentshop.service.*;
import com.agentshop.event.ShoppingEvents.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shopping tools that the AI agent can invoke via Spring AI function calling.
 * Each method is a tool the LLM can call during conversation.
 */
@Component
public class ShoppingTools {

    private static final Logger log = LoggerFactory.getLogger(ShoppingTools.class);
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final OrderService orderService;
    private final RazorpayService razorpayService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;
    private final com.agentshop.service.checkout.CheckoutPipeline checkoutPipeline;
    private final CouponService couponService;

    public ShoppingTools(ProductRepository productRepository, CartService cartService, OrderService orderService, RazorpayService razorpayService, ApplicationEventPublisher eventPublisher, ObjectMapper objectMapper, com.agentshop.service.checkout.CheckoutPipeline checkoutPipeline, CouponService couponService) {
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.orderService = orderService;
        this.razorpayService = razorpayService;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
        this.checkoutPipeline = checkoutPipeline;
        this.couponService = couponService;
    }

    @Value("${agentshop.max-order-amount:5000000}")
    private long maxOrderAmountPaise;

    // ---- Current session tracking using ThreadLocal to ensure thread-safety ----
    private static final ThreadLocal<String> currentSessionIdThreadLocal = ThreadLocal.withInitial(() -> "default-session");

    public void setCurrentSessionId(String sessionId) {
        currentSessionIdThreadLocal.set(sessionId);
    }

    private String getSessionId() {
        return currentSessionIdThreadLocal.get();
    }

    public void clearCurrentSessionId() {
        currentSessionIdThreadLocal.remove();
    }

    /**
     * Search products by query, optional category, and optional max price.
     */
    public String searchProducts(String query, String category, Double maxPrice) {
        log.info("🔍 Tool: searchProducts(query={}, category={}, maxPrice={})", query, category, maxPrice);

        List<Product> results;
        if (maxPrice != null && maxPrice > 0) {
            results = productRepository.searchProductsWithMaxPrice(query, BigDecimal.valueOf(maxPrice));
        } else {
            results = productRepository.searchProducts(query);
        }

        // If exact phrase has no match, try token-based search
        if (results.isEmpty() && query != null && query.contains(" ")) {
            String[] tokens = query.split("\\s+");
            Set<Product> tokenResults = new LinkedHashSet<>();
            for (String token : tokens) {
                if (token.length() >= 2) {
                    if (maxPrice != null && maxPrice > 0) {
                        tokenResults.addAll(productRepository.searchProductsWithMaxPrice(token, BigDecimal.valueOf(maxPrice)));
                    } else {
                        tokenResults.addAll(productRepository.searchProducts(token));
                    }
                }
            }
            results = new ArrayList<>(tokenResults);
        }

        if (category != null && !category.isEmpty()) {
            results = results.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(category))
                    .collect(Collectors.toList());
        }

        // Limit to top 5 results
        results = results.stream().limit(5).collect(Collectors.toList());

        eventPublisher.publishEvent(new ProductSearchedEvent(getSessionId(), query, category, maxPrice, results));

        if (results.isEmpty()) {
            return "No products found matching '" + query + "'. Try a broader search term.";
        }

        try {
            // Inject matchScore for AI recommendations
            List<Map<String, Object>> productMaps = new ArrayList<>();
            for (int i = 0; i < results.size(); i++) {
                Product p = results.get(i);
                Map<String, Object> map = objectMapper.convertValue(p, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
                // Calculate synthetic match score (90-99% based on rank and rating)
                int baseScore = 95 - (i * 2);
                int score = Math.min(99, baseScore + (int)(p.getRating() != null ? p.getRating() : 4.0));
                map.put("matchScore", score);
                productMaps.add(map);
            }
            String json = objectMapper.writeValueAsString(productMaps);
            return "__PRODUCT_LIST_START__\n" + json + "\n__PRODUCT_LIST_END__";
        } catch (Exception e) {
            log.error("Failed to serialize products", e);
            return "Found " + results.size() + " products.";
        }
    }

    /**
     * Get detailed product information.
     */
    public String getProductDetails(Long productId) {
        log.info("📋 Tool: getProductDetails(productId={})", productId);

        Product p = productRepository.findById(productId)
                .orElse(null);

        if (p == null) {
            return "Product with ID " + productId + " not found.";
        }

        eventPublisher.publishEvent(new ProductViewedEvent(getSessionId(), p));

        try {
            String json = objectMapper.writeValueAsString(java.util.List.of(p));
            return "__PRODUCT_LIST_START__\n" + json + "\n__PRODUCT_LIST_END__";
        } catch (Exception e) {
            log.error("Failed to serialize product details", e);
            return "Product details for " + p.getName();
        }
    }

    /**
     * Add a product to the shopping cart.
     */
    public String addToCart(Long productId, Integer quantity) {
        log.info("🛒 Tool: addToCart(productId={}, quantity={})", productId, quantity);
        int qty = (quantity != null && quantity > 0) ? quantity : 1;

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return "Product with ID " + productId + " not found.";
        }

        if (product.getStock() < qty) {
            eventPublisher.publishEvent(new CartAddFailedEvent(getSessionId(), product, qty, "Insufficient stock"));
            return "Sorry, only " + product.getStock() + " units of " + product.getName() + " are available.";
        }

        CartItem item = cartService.addToCart(getSessionId(), productId, qty);
        String cartSummary = cartService.getCartSummary(getSessionId());

        eventPublisher.publishEvent(new CartItemAddedEvent(getSessionId(), product, qty, item, cartService.getCartTotal(getSessionId())));

        // Get upsell recommendations
        String upsellSuggestion = getUpsellRecommendations(productId);

        // Proactively suggest applicable coupons
        BigDecimal cartTotal = cartService.getCartTotal(getSessionId());
        String couponSuggestions = couponService.getProactiveSuggestions(cartTotal, List.of(product.getCategory()));

        return "✅ Added **" + product.getName() + "** (x" + qty + ") to your cart!\n\n" +
                cartSummary + "\n\n" +
                "---\n" + upsellSuggestion + couponSuggestions;
    }

    /**
     * Remove a product from the cart.
     */
    public String removeFromCart(Long productId) {
        log.info("❌ Tool: removeFromCart(productId={})", productId);

        Product product = productRepository.findById(productId).orElse(null);
        String productName = product != null ? product.getName() : "Product #" + productId;

        cartService.removeFromCart(getSessionId(), productId);

        eventPublisher.publishEvent(new CartItemRemovedEvent(getSessionId(), product));

        String cartSummary = cartService.getCartSummary(getSessionId());
        return "Removed **" + productName + "** from your cart.\n\n" + cartSummary;
    }

    /**
     * View the current cart.
     */
    public String viewCart() {
        log.info("📦 Tool: viewCart()");

        eventPublisher.publishEvent(new CartViewedEvent(getSessionId(), cartService.getCartTotal(getSessionId())));

        return cartService.getCartSummary(getSessionId());
    }

    /**
     * Initiate checkout — creates Razorpay order and payment link.
     */
    public String initiateCheckout(String customerName, String customerEmail, String customerPhone, String deliveryAddress) {
        log.info("💳 Tool: initiateCheckout(name={}, email={}, address={})", customerName, customerEmail, deliveryAddress);
        List<CartItem> cartItems = cartService.getCart(getSessionId());
        BigDecimal cartTotal = cartService.getCartTotal(getSessionId());
        
        BigDecimal discount = couponService.getDiscountForSession(getSessionId(), cartTotal);
        BigDecimal finalAmount = cartTotal.subtract(discount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) finalAmount = BigDecimal.ZERO;
        
        com.agentshop.service.checkout.CheckoutContext ctx = new com.agentshop.service.checkout.CheckoutContext(
                getSessionId(), customerName, customerEmail, customerPhone, deliveryAddress, cartItems, finalAmount, discount);
                
        return checkoutPipeline.execute(ctx);
    }

    /**
     * Get complementary categories for cross-selling based on the primary category.
     */
    private List<String> getComplementaryCategories(String category) {
        return switch (category) {
            case "Laptops" -> List.of("Headphones", "Smartwatches");
            case "Smartphones" -> List.of("Headphones", "Smartwatches");
            case "Tablets" -> List.of("Headphones", "Laptops");
            case "Headphones" -> List.of("Smartphones", "Smartwatches");
            case "Cameras" -> List.of("Laptops", "Tablets");
            case "Smartwatches" -> List.of("Smartphones", "Headphones");
            default -> List.of();
        };
    }

    /**
     * Get upsell/cross-sell recommendations for a product.
     */
    public String getUpsellRecommendations(Long productId) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) return "";

        // 1. Get Upsells (Same Category)
        List<Product> related = productRepository.findRelatedProducts(product.getCategory(), productId)
                .stream().limit(1).collect(Collectors.toList());

        // 2. Get Cross-Sells (Complementary Categories)
        List<String> crossSellCategories = getComplementaryCategories(product.getCategory());
        if (!crossSellCategories.isEmpty()) {
            List<Product> crossSells = productRepository.findCrossSellProducts(crossSellCategories, productId)
                    .stream().limit(2).toList();
            related.addAll(crossSells);
        }

        if (related.isEmpty()) return "No additional recommendations available.";

        eventPublisher.publishEvent(new UpsellOfferedEvent(getSessionId(), product, related));

        StringBuilder sb = new StringBuilder("💡 **You might also like:**\n");
        for (Product p : related) {
            sb.append(String.format("  • **%s** — ₹%s (⭐%.1f) [Product ID: %d]\n",
                    p.getName(), p.getPrice().toPlainString(), p.getRating(), p.getId()));
        }
        sb.append("\n_Say 'add product [ID]' to add any of these to your cart!_\n");
        sb.append("💳 **Pro Tip:** You get an extra **15% discount** on all products when paying online via Razorpay! (Use Code: `RAZORPAY15`)");
        return sb.toString();
    }

    /**
     * List all available product categories.
     */
    public String listCategories() {
        List<String> categories = productRepository.findAllCategories();
        
        eventPublisher.publishEvent(new CategoriesListedEvent(getSessionId(), categories));

        StringBuilder sb = new StringBuilder("📂 **Available Categories:**\n\n");
        for (String cat : categories) {
            sb.append("  • " + cat + "\n");
        }
        sb.append("\n_Ask me to show products in any category!_");
        return sb.toString();
    }

    /**
     * Simulate payment status check (for demo purposes).
     */
    public String checkPaymentStatus(Long orderId) {
        log.info("🔍 Tool: checkPaymentStatus(orderId={})", orderId);
        Optional<Order> optOrder = Optional.empty();
        List<Order> orders = orderService.getOrdersBySession(getSessionId());
        for (Order o : orders) {
            if (o.getId().equals(orderId)) {
                optOrder = Optional.of(o);
                break;
            }
        }

        if (optOrder.isEmpty()) {
            return "Order #" + orderId + " not found.";
        }

        Order order = optOrder.get();
        return String.format("Order #%d — Status: **%s** | Amount: ₹%s | Razorpay ID: %s",
                order.getId(), order.getStatus(), order.getTotalAmount().toPlainString(),
                order.getRazorpayOrderId() != null ? order.getRazorpayOrderId() : "N/A");
    }

    /**
     * List all available coupons and promotional offers.
     */
    public String listCoupons() {
        log.info("🏷️ Tool: listCoupons()");
        return couponService.listAvailableCoupons();
    }

    /**
     * Apply a coupon code to the current cart.
     */
    public String applyCoupon(String couponCode) {
        log.info("🏷️ Tool: applyCoupon(code={})", couponCode);
        BigDecimal cartTotal = cartService.getCartTotal(getSessionId());
        if (cartTotal.compareTo(BigDecimal.ZERO) == 0) {
            return "❌ Your cart is empty. Add items before applying a coupon.";
        }
        CouponService.CouponResult result = couponService.applyCoupon(getSessionId(), couponCode, cartTotal);
        return result.message();
    }

    /**
     * Remove the currently applied coupon.
     */
    public String removeCoupon() {
        log.info("🏷️ Tool: removeCoupon()");
        return couponService.removeCoupon(getSessionId());
    }
}
