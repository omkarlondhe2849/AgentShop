package com.agentshop.event;

import com.agentshop.model.AuditLog;
import com.agentshop.model.Product;
import com.agentshop.service.AuditService;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AuditEventListener {

    private final AuditService auditService;

    public AuditEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    @Async
    public void onProductSearched(ShoppingEvents.ProductSearchedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.PRODUCT_SEARCH,
                "Searched for: " + event.query() + (event.category() != null ? " in " + event.category() : ""),
                "User is looking for products matching '" + event.query() + "'. Returning top " + event.results().size() + " results.",
                Map.of("query", event.query(), "category", event.category() != null ? event.category() : "all", "maxPrice", event.maxPrice() != null ? event.maxPrice() : "none"),
                Map.of("resultCount", event.results().size()),
                AuditLog.ActionStatus.SUCCESS, true, "Read-only search operation");
    }

    @EventListener
    @Async
    public void onProductViewed(ShoppingEvents.ProductViewedEvent event) {
        Product p = event.product();
        auditService.logAction(event.sessionId(), AuditLog.ActionType.PRODUCT_VIEW,
                "Viewed product: " + p.getName(),
                "User requested details about product #" + p.getId(),
                Map.of("productId", p.getId()),
                Map.of("productName", p.getName(), "price", p.getPrice()),
                AuditLog.ActionStatus.SUCCESS, true, "Read-only operation");
    }

    @EventListener
    @Async
    public void onCartItemAdded(ShoppingEvents.CartItemAddedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.ADD_TO_CART,
                "Added " + event.quantity() + "x " + event.product().getName() + " to cart",
                "User wants to add " + event.product().getName() + " to their cart. Stock check passed.",
                Map.of("productId", event.product().getId(), "productName", event.product().getName(), "quantity", event.quantity(), "price", event.product().getPrice()),
                Map.of("cartItemId", event.cartItem().getId(), "newCartTotal", event.newCartTotal()),
                AuditLog.ActionStatus.SUCCESS, true, "Stock available: " + event.product().getStock() + " >= " + event.quantity());
    }

    @EventListener
    @Async
    public void onCartAddFailed(ShoppingEvents.CartAddFailedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.ADD_TO_CART,
                "Failed to add " + event.product().getName() + " — insufficient stock",
                "Requested quantity " + event.quantity() + " exceeds available stock " + event.product().getStock(),
                Map.of("productId", event.product().getId(), "quantity", event.quantity()),
                Map.of("error", event.reason()),
                AuditLog.ActionStatus.FAILURE, false, "Stock check failed: " + event.quantity() + " > " + event.product().getStock());
    }

    @EventListener
    @Async
    public void onCartItemRemoved(ShoppingEvents.CartItemRemovedEvent event) {
        String productName = event.product() != null ? event.product().getName() : "Unknown Product";
        Long productId = event.product() != null ? event.product().getId() : -1L;
        auditService.logAction(event.sessionId(), AuditLog.ActionType.REMOVE_FROM_CART,
                "Removed " + productName + " from cart",
                "User requested removal of product from cart",
                Map.of("productId", productId), null,
                AuditLog.ActionStatus.SUCCESS, true, "Cart modification — item removal");
    }

    @EventListener
    @Async
    public void onCartViewed(ShoppingEvents.CartViewedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.CART_VIEW,
                "Viewed cart", "User wants to see their cart contents",
                null, Map.of("cartTotal", event.cartTotal()),
                AuditLog.ActionStatus.SUCCESS, true, "Read-only operation");
    }

    @EventListener
    @Async
    public void onCheckoutInitiated(ShoppingEvents.CheckoutInitiatedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.CHECKOUT_INITIATED,
                "Checkout initiated — Order #" + event.order().getId() + " created for ₹" + event.order().getTotalAmount(),
                "User confirmed checkout. Creating Razorpay order and payment link.",
                Map.of("orderId", event.order().getId(), "total", event.order().getTotalAmount(), "items", event.itemCount()),
                null, AuditLog.ActionStatus.PENDING, true, "User confirmation received");
    }

    @EventListener
    @Async
    public void onCheckoutBlocked(ShoppingEvents.CheckoutBlockedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.ORDER_AMOUNT_CHECK,
                "Order blocked — exceeds maximum amount ₹" + (event.maxAmountPaise() / 100),
                "Cart total ₹" + event.cartTotal() + " exceeds the maximum allowed order of ₹" + (event.maxAmountPaise() / 100) + ". Blocking checkout.",
                Map.of("cartTotal", event.cartTotal(), "maxAllowed", event.maxAmountPaise() / 100),
                Map.of("blocked", true),
                AuditLog.ActionStatus.BLOCKED, false,
                "BOUNDARY VIOLATED: ₹" + event.cartTotal() + " > ₹" + (event.maxAmountPaise() / 100));
    }

    @EventListener
    @Async
    public void onPaymentLinkCreated(ShoppingEvents.PaymentLinkCreatedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.ORDER_AMOUNT_CHECK,
                "Order amount ₹" + event.order().getTotalAmount() + " within limits",
                "Verified order total is within the allowed maximum",
                Map.of("cartTotal", event.order().getTotalAmount()),
                Map.of("passed", true),
                AuditLog.ActionStatus.SUCCESS, true,
                "Order total within bounds");

        auditService.logAction(event.sessionId(), AuditLog.ActionType.PAYMENT_LINK_CREATED,
                "Payment link created: " + event.shortUrl(),
                "Razorpay order and payment link generated successfully. Awaiting customer payment.",
                Map.of("razorpayOrderId", event.razorpayOrderId(), "paymentLinkId", event.paymentLinkId()),
                Map.of("paymentUrl", event.shortUrl(), "amount", event.order().getTotalAmount()),
                AuditLog.ActionStatus.SUCCESS, true, "Payment link generated — bounded to ₹" + event.order().getTotalAmount());
    }

    @EventListener
    @Async
    public void onPaymentLinkFailed(ShoppingEvents.PaymentLinkFailedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.PAYMENT_LINK_CREATED,
                "Payment link creation FAILED: " + event.errorMessage(),
                "Razorpay API call failed. Will offer retry option to customer.",
                Map.of("orderId", event.order().getId(), "error", event.errorMessage()),
                null, AuditLog.ActionStatus.FAILURE, true, "API error — no money was charged");
    }

    @EventListener
    @Async
    public void onUpsellOffered(ShoppingEvents.UpsellOfferedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.UPSELL_OFFERED,
                "Offered " + event.suggestions().size() + " upsell items for " + event.baseProduct().getName(),
                "After adding " + event.baseProduct().getName() + " to cart, suggesting related products to increase order value.",
                Map.of("baseProductId", event.baseProduct().getId(), "baseProductName", event.baseProduct().getName()),
                Map.of("suggestions", event.suggestions().stream().map(Product::getName).collect(Collectors.toList())),
                AuditLog.ActionStatus.SUCCESS, true, "Upsell is a suggestion only — no auto-add");
    }

    @EventListener
    @Async
    public void onCategoriesListed(ShoppingEvents.CategoriesListedEvent event) {
        auditService.logAction(event.sessionId(), AuditLog.ActionType.PRODUCT_SEARCH,
                "Listed all categories",
                "User wants to browse by category",
                null, Map.of("categories", event.categories()),
                AuditLog.ActionStatus.SUCCESS, true, "Read-only operation");
    }
}
