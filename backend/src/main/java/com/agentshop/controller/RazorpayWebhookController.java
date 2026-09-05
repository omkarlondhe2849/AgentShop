package com.agentshop.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agentshop.model.AuditLog;
import com.agentshop.model.Order;
import com.agentshop.service.AuditService;
import com.agentshop.service.CartService;
import com.agentshop.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class RazorpayWebhookController {

    private static final Logger log = LoggerFactory.getLogger(RazorpayWebhookController.class);

    private final OrderService orderService;
    private final AuditService auditService;
    private final CartService cartService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.agentshop.repository.ConversationRepository conversationRepository;

    public RazorpayWebhookController(OrderService orderService, AuditService auditService, CartService cartService, SimpMessagingTemplate messagingTemplate, com.agentshop.repository.ConversationRepository conversationRepository) {
        this.orderService = orderService;
        this.auditService = auditService;
        this.cartService = cartService;
        this.messagingTemplate = messagingTemplate;
        this.conversationRepository = conversationRepository;
    }

    @PostMapping("/razorpay")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("📨 Razorpay webhook received");

        try {
            // Parse the webhook payload
            org.json.JSONObject json = new org.json.JSONObject(payload);
            String event = json.getString("event");

            log.info("Webhook event: {}", event);

            if ("payment.captured".equals(event) || "payment_link.paid".equals(event)) {
                handlePaymentSuccess(json);
            } else if ("payment.failed".equals(event)) {
                handlePaymentFailure(json);
            }

            return ResponseEntity.ok("OK");

        } catch (Exception e) {
            log.error("Webhook processing error", e);
            return ResponseEntity.ok("OK"); // Always return 200 to Razorpay
        }
    }

    /**
     * Manual endpoint to simulate payment success (for demo without real webhooks).
     */
    @PostMapping("/simulate/payment-success")
    public ResponseEntity<Map<String, Object>> simulatePaymentSuccess(
            @RequestParam String razorpayOrderId) {

        log.info("🎯 Simulating payment success for: {}", razorpayOrderId);

        try {
            Order order = orderService.markPaid(razorpayOrderId, "pay_simulated_" + System.currentTimeMillis());

            // Clear the cart
            cartService.clearCart(order.getSessionId());

            auditService.logAction(order.getSessionId(), AuditLog.ActionType.PAYMENT_SUCCESS,
                    "Payment successful for Order #" + order.getId() + " — ₹" + order.getTotalAmount(),
                    "Payment was captured successfully. Cart has been cleared.",
                    Map.of("razorpayOrderId", razorpayOrderId),
                    Map.of("orderId", order.getId(), "amount", order.getTotalAmount(), "status", "PAID"),
                    AuditLog.ActionStatus.SUCCESS, true, "Payment captured within allowed bounds");

            // Save chat message
            String successMsg = String.format("🎉 **Payment Verified & Captured!**\n\nOrder ID: #%d\nAmount Paid: ₹%s\nStatus: PAID (Verified via Razorpay Webhook)\n\nYour items will be shipped shortly. Thank you for shopping with AgentShop! 🚚", order.getId(), order.getTotalAmount().toPlainString());
            com.agentshop.model.Conversation conv = new com.agentshop.model.Conversation();
            conv.setSessionId(order.getSessionId());
            conv.setRole(com.agentshop.model.Conversation.Role.ASSISTANT);
            conv.setContent(successMsg);
            conversationRepository.save(conv);

            // Notify via WebSocket
            messagingTemplate.convertAndSend("/topic/payment/" + order.getSessionId(),
                    Map.of("status", "PAID", "orderId", order.getId(), "amount", order.getTotalAmount()));

            return ResponseEntity.ok(Map.of(
                    "status", "PAID",
                    "orderId", order.getId(),
                    "amount", order.getTotalAmount(),
                    "message", "Payment simulated successfully!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Manual endpoint to simulate payment failure (for demo).
     */
    @PostMapping("/simulate/payment-failure")
    public ResponseEntity<Map<String, Object>> simulatePaymentFailure(
            @RequestParam String razorpayOrderId) {

        log.info("❌ Simulating payment failure for: {}", razorpayOrderId);

        try {
            Order order = orderService.markFailed(razorpayOrderId);

            auditService.logAction(order.getSessionId(), AuditLog.ActionType.PAYMENT_FAILED,
                    "Payment FAILED for Order #" + order.getId() + " — ₹" + order.getTotalAmount(),
                    "Payment was declined. Retry count: " + order.getRetryCount() + ". Will offer customer a retry option.",
                    Map.of("razorpayOrderId", razorpayOrderId, "retryCount", order.getRetryCount()),
                    Map.of("orderId", order.getId(), "status", "FAILED"),
                    AuditLog.ActionStatus.FAILURE, true,
                    "Payment failed — no money was charged. Retry available.");

            // Save chat message
            String failMsg = String.format("❌ **Payment Failed!**\n\nOrder ID: #%d\nStatus: FAILED\n\nPlease try again.", order.getId());
            com.agentshop.model.Conversation conv = new com.agentshop.model.Conversation();
            conv.setSessionId(order.getSessionId());
            conv.setRole(com.agentshop.model.Conversation.Role.ASSISTANT);
            conv.setContent(failMsg);
            conversationRepository.save(conv);

            // Notify via WebSocket
            messagingTemplate.convertAndSend("/topic/payment/" + order.getSessionId(),
                    Map.of("status", "FAILED", "orderId", order.getId(), "retryCount", order.getRetryCount()));

            return ResponseEntity.ok(Map.of(
                    "status", "FAILED",
                    "orderId", order.getId(),
                    "retryCount", order.getRetryCount(),
                    "message", "Payment failure simulated!"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ---- Private handlers ----

    private void handlePaymentSuccess(org.json.JSONObject json) {
        try {
            org.json.JSONObject paymentEntity = json.getJSONObject("payload")
                    .getJSONObject("payment").getJSONObject("entity");

            String orderId = paymentEntity.optString("order_id");
            String paymentId = paymentEntity.getString("id");

            if (!orderId.isEmpty()) {
                Order order = orderService.markPaid(orderId, paymentId);
                cartService.clearCart(order.getSessionId());

                auditService.logAction(order.getSessionId(), AuditLog.ActionType.PAYMENT_SUCCESS,
                        "Payment captured: " + paymentId,
                        "Webhook confirmed payment capture. Order fulfilled.",
                        Map.of("paymentId", paymentId, "orderId", orderId),
                        Map.of("orderStatus", "PAID"),
                        AuditLog.ActionStatus.SUCCESS, true, "Verified via Razorpay webhook");

                // Save chat message
                String successMsg = String.format("🎉 **Payment Verified & Captured!**\n\nOrder ID: #%s\nStatus: PAID (Verified via Razorpay Webhook)\n\nYour items will be shipped shortly. Thank you for shopping with AgentShop! 🚚", order.getId());
                com.agentshop.model.Conversation conv = new com.agentshop.model.Conversation();
                conv.setSessionId(order.getSessionId());
                conv.setRole(com.agentshop.model.Conversation.Role.ASSISTANT);
                conv.setContent(successMsg);
                conversationRepository.save(conv);

                messagingTemplate.convertAndSend("/topic/payment/" + order.getSessionId(),
                        Map.of("status", "PAID", "orderId", order.getId()));
            }
        } catch (Exception e) {
            log.error("Error handling payment success webhook", e);
        }
    }

    private void handlePaymentFailure(org.json.JSONObject json) {
        try {
            org.json.JSONObject paymentEntity = json.getJSONObject("payload")
                    .getJSONObject("payment").getJSONObject("entity");

            String orderId = paymentEntity.optString("order_id");

            if (!orderId.isEmpty()) {
                Order order = orderService.markFailed(orderId);

                auditService.logAction(order.getSessionId(), AuditLog.ActionType.PAYMENT_FAILED,
                        "Payment failed for order: " + orderId,
                        "Webhook reported payment failure. Will offer retry to customer.",
                        Map.of("orderId", orderId),
                        Map.of("retryCount", order.getRetryCount()),
                        AuditLog.ActionStatus.FAILURE, true, "No money charged — retry possible");

                // Save chat message
                String failMsg = String.format("❌ **Payment Failed!**\n\nOrder ID: #%s\nStatus: FAILED\n\nPlease try again.", order.getId());
                com.agentshop.model.Conversation conv = new com.agentshop.model.Conversation();
                conv.setSessionId(order.getSessionId());
                conv.setRole(com.agentshop.model.Conversation.Role.ASSISTANT);
                conv.setContent(failMsg);
                conversationRepository.save(conv);

                messagingTemplate.convertAndSend("/topic/payment/" + order.getSessionId(),
                        Map.of("status", "FAILED", "orderId", order.getId(), "retryCount", order.getRetryCount()));
            }
        } catch (Exception e) {
            log.error("Error handling payment failure webhook", e);
        }
    }
}
