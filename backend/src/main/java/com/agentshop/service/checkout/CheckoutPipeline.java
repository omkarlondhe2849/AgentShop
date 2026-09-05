package com.agentshop.service.checkout;

import com.agentshop.model.Order;
import com.agentshop.service.OrderService;
import com.agentshop.service.RazorpayService;
import com.agentshop.event.ShoppingEvents.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CheckoutPipeline {

    private static final Logger log = LoggerFactory.getLogger(CheckoutPipeline.class);

    private final List<CheckoutValidator> validators;
    private final OrderService orderService;
    private final RazorpayService razorpayService;
    private final ApplicationEventPublisher eventPublisher;

    /** Idempotency guard: tracks in-flight checkouts per session to prevent duplicate payment links. */
    private final Map<String, Long> activeCheckouts = new ConcurrentHashMap<>();
    private static final long CHECKOUT_COOLDOWN_MS = 60_000; // 60 second cooldown

    public CheckoutPipeline(List<CheckoutValidator> validators, 
                            OrderService orderService, 
                            RazorpayService razorpayService, 
                            ApplicationEventPublisher eventPublisher) {
        this.validators = validators.stream()
                .sorted(Comparator.comparingInt(CheckoutValidator::order))
                .toList();
        this.orderService = orderService;
        this.razorpayService = razorpayService;
        this.eventPublisher = eventPublisher;
    }

    public String execute(CheckoutContext ctx) {
        // Idempotency check: prevent duplicate checkouts within cooldown window
        Long lastCheckout = activeCheckouts.get(ctx.sessionId());
        long now = System.currentTimeMillis();
        if (lastCheckout != null && (now - lastCheckout) < CHECKOUT_COOLDOWN_MS) {
            long remainingSec = (CHECKOUT_COOLDOWN_MS - (now - lastCheckout)) / 1000;
            log.warn("🛑 Idempotency guard: duplicate checkout blocked for session {} ({}s remaining)",
                    ctx.sessionId(), remainingSec);
            return "⚠️ A checkout is already in progress for this session. " +
                   "Please wait " + remainingSec + " seconds or use the existing payment link.";
        }

        for (CheckoutValidator validator : validators) {
            ValidationResult result = validator.validate(ctx);
            if (!result.passed()) {
                return result.reason();
            }
        }

        // Mark checkout as active before proceeding
        activeCheckouts.put(ctx.sessionId(), now);
        return proceedWithPayment(ctx);
    }

    private String proceedWithPayment(CheckoutContext ctx) {
        long totalPaise = ctx.totalAmount().multiply(BigDecimal.valueOf(100)).longValue();

        Order order = orderService.createOrder(ctx.sessionId(),
                ctx.customerName() != null ? ctx.customerName() : "Customer",
                ctx.customerEmail() != null ? ctx.customerEmail() : "customer@agentshop.com",
                ctx.customerPhone() != null ? ctx.customerPhone() : "9999999999",
                ctx.deliveryAddress(),
                ctx.discountAmount());

        eventPublisher.publishEvent(new CheckoutInitiatedEvent(ctx.sessionId(), order, ctx.cartItems().size()));

        try {
            JSONObject rzpOrder = razorpayService.createOrder(
                    ctx.sessionId(), (int) totalPaise, "INR", "order_" + order.getId());
            String rzpOrderId = rzpOrder.getString("id");

            String description = "AgentShop Order #" + order.getId() + " — " + ctx.cartItems().size() + " items";
            JSONObject paymentLink = razorpayService.createPaymentLink(
                    ctx.sessionId(), (int) totalPaise,
                    order.getCustomerName(), order.getCustomerEmail(), order.getCustomerPhone(),
                    description, rzpOrderId);

            String linkId = paymentLink.getString("id");
            String shortUrl = paymentLink.getString("short_url");

            orderService.updateOrderWithRazorpay(order.getId(), rzpOrderId, linkId, shortUrl);
            eventPublisher.publishEvent(new PaymentLinkCreatedEvent(ctx.sessionId(), order, rzpOrderId, linkId, shortUrl));

            return String.format("""
                    ✅ **Order Created Successfully!**
                    
                    📋 Order #%d
                    💰 Total: ₹%s
                    
                    🔗 **[Click here to pay](%s)**
                    
                    Payment Link: %s
                    
                    _This payment link is valid for 30 minutes. The amount is fixed at ₹%s and cannot be modified._
                    """, order.getId(), ctx.totalAmount().toPlainString(), shortUrl, shortUrl, ctx.totalAmount().toPlainString());

        } catch (Exception e) {
            eventPublisher.publishEvent(new PaymentLinkFailedEvent(ctx.sessionId(), order, e.getMessage()));
            return "❌ Sorry, there was an issue creating your payment link. Error: " + e.getMessage() +
                    "\n\nWould you like me to try again?";
        }
    }
}
