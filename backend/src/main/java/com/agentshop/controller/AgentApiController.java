package com.agentshop.controller;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.model.CartItem;
import com.agentshop.model.Order;
import com.agentshop.model.Conversation;
import com.agentshop.repository.ConversationRepository;
import com.agentshop.service.CartService;
import com.agentshop.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent-to-Agent programmatic API endpoints.
 * Enables external autonomous AI agents to manage carts, initiate bounded checkout,
 * and track payment state via REST.
 */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
public class AgentApiController {

    private final CartService cartService;
    private final OrderService orderService;
    private final ShoppingTools shoppingTools;
    private final ConversationRepository conversationRepository;

    public AgentApiController(CartService cartService, OrderService orderService, ShoppingTools shoppingTools, ConversationRepository conversationRepository) {
        this.cartService = cartService;
        this.orderService = orderService;
        this.shoppingTools = shoppingTools;
        this.conversationRepository = conversationRepository;
    }

    public record AddCartRequest(String sessionId, Long productId, Integer quantity) {}
    public record CheckoutRequest(String sessionId, String customerName, String customerEmail, String deliveryAddress) {}

    @PostMapping("/cart")
    public ResponseEntity<Map<String, Object>> addToCart(@RequestBody AddCartRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : "default-agent-session";
        shoppingTools.setCurrentSessionId(sessionId);
        String result = shoppingTools.addToCart(
                request.productId(),
                request.quantity() != null ? request.quantity() : 1
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("@context", "https://schema.org");
        response.put("@type", "CartActionResult");
        response.put("status", "SUCCESS");
        response.put("message", result);
        
        List<com.agentshop.dto.CartItemResponse> cartResponse = cartService.getCart(sessionId).stream()
                .map(com.agentshop.dto.CartItemResponse::from)
                .toList();
        response.put("cart", cartResponse);
        response.put("totalAmount", cartService.getCartTotal(sessionId));
        response.put("_actions", List.of(
                Map.of("name", "checkout", "method", "POST", "href", "/api/v1/checkout",
                        "body", Map.of("sessionId", sessionId)),
                Map.of("name", "viewCart", "method", "GET", "href", "/api/v1/cart/" + sessionId)
        ));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/cart/{sessionId}")
    public ResponseEntity<Map<String, Object>> getCart(@PathVariable String sessionId) {
        List<CartItem> items = cartService.getCart(sessionId);
        BigDecimal total = cartService.getCartTotal(sessionId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("@context", "https://schema.org");
        response.put("@type", "Order");
        response.put("orderStatus", "OrderDraft");
        response.put("sessionId", sessionId);
        
        List<com.agentshop.dto.CartItemResponse> cartResponse = items.stream()
                .map(com.agentshop.dto.CartItemResponse::from)
                .toList();
        response.put("items", cartResponse);
        response.put("totalPrice", total);
        response.put("currency", "INR");
        response.put("_actions", List.of(
                Map.of("name", "checkout", "method", "POST", "href", "/api/v1/checkout",
                        "body", Map.of("sessionId", sessionId))
        ));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/cart/{sessionId}/items/{productId}")
    public ResponseEntity<Map<String, Object>> removeFromCart(@PathVariable String sessionId, @PathVariable Long productId) {
        shoppingTools.setCurrentSessionId(sessionId);
        String result = shoppingTools.removeFromCart(productId);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "SUCCESS");
        response.put("message", result);
        
        List<com.agentshop.dto.CartItemResponse> cartResponse = cartService.getCart(sessionId).stream()
                .map(com.agentshop.dto.CartItemResponse::from)
                .toList();
        response.put("cart", cartResponse);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/checkout")
    public ResponseEntity<Map<String, Object>> checkout(@RequestBody CheckoutRequest request) {
        String sessionId = request.sessionId() != null ? request.sessionId() : "default-agent-session";
        shoppingTools.setCurrentSessionId(sessionId);
        
        // Persist the user's synthetic address submission message
        String address = request.deliveryAddress() != null ? request.deliveryAddress() : "unknown";
        conversationRepository.save(Conversation.builder()
                .sessionId(sessionId)
                .role(Conversation.Role.USER)
                .content("Deliver to: " + address)
                .timestamp(java.time.LocalDateTime.now())
                .build());

        String result = shoppingTools.initiateCheckout(
                request.customerName() != null ? request.customerName() : "AI Buyer Agent",
                request.customerEmail() != null ? request.customerEmail() : "agent@buyer.ai",
                "9876543210",
                address
        );

        // Persist the assistant's payment link response
        conversationRepository.save(Conversation.builder()
                .sessionId(sessionId)
                .role(Conversation.Role.ASSISTANT)
                .content(result)
                .timestamp(java.time.LocalDateTime.now())
                .build());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("@context", "https://schema.org");
        response.put("@type", "PaymentAction");
        response.put("status", "SUCCESS");
        response.put("message", result);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/orders/{orderId}/status")
    public ResponseEntity<Map<String, Object>> getOrderStatus(@PathVariable Long orderId) {
        return orderService.findById(orderId)
                .map(order -> {
                    Map<String, Object> resp = new LinkedHashMap<String, Object>();
                    resp.put("@type", "Order");
                    resp.put("orderId", order.getId());
                    resp.put("status", order.getStatus());
                    resp.put("totalAmount", order.getTotalAmount());
                    resp.put("razorpayPaymentLinkId", order.getRazorpayPaymentLinkId());
                    resp.put("paidAt", order.getPaidAt());
                    return ResponseEntity.ok(resp);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
