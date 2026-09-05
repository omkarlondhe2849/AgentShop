package com.agentshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agentshop.agent.ShoppingTools;
import com.agentshop.model.Conversation;
import com.agentshop.model.Product;
import com.agentshop.repository.ConversationRepository;
import com.agentshop.repository.ProductRepository;
import com.agentshop.service.agent.AgentOrchestrator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    // Token optimization: only keep last N messages in LLM context
    private static final int MAX_HISTORY_MESSAGES = 5;

    private final ChatClient.Builder chatClientBuilder;
    private final ConversationRepository conversationRepository;
    private final ShoppingTools shoppingTools;
    private final AuditService auditService;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final AgentOrchestrator agentOrchestrator;

    public ChatService(ChatClient.Builder chatClientBuilder,
                       ConversationRepository conversationRepository,
                       ShoppingTools shoppingTools,
                       AuditService auditService,
                       ProductRepository productRepository,
                       CartService cartService,
                       AgentOrchestrator agentOrchestrator) {
        this.chatClientBuilder = chatClientBuilder;
        this.conversationRepository = conversationRepository;
        this.shoppingTools = shoppingTools;
        this.auditService = auditService;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.agentOrchestrator = agentOrchestrator;
    }

    // Token-optimized system prompt (~200 tokens instead of ~400)
    private static final String SYSTEM_PROMPT = """
            You are ShopBot 🤖 for AgentShop (Indian e-commerce, Razorpay payments).
            Multi-agent system: Sales Agent (search/upsell), Support Agent (RAG FAQ), Checkout Agent (cart/payment).
            Be warm, use emojis. Match user's language (Hindi/Tamil/etc). Proactively suggest coupons.
            Tools: searchProducts, getProductDetails, addToCart, removeFromCart, viewCart, initiateCheckout, getUpsellRecommendations, listCategories, checkPaymentStatus, listCoupons, applyCoupon, removeCoupon.
            Rules: Never checkout without confirmation. Max order ₹50,000. Always suggest upsells after adding to cart.
            """;

    public String chat(String sessionId, String userMessage) {
        return processMessage(sessionId, userMessage);
    }

    public String processMessage(String sessionId, String userMessage) {
        log.info("💬 Chat [{}]: {}", sessionId, userMessage);

        shoppingTools.setCurrentSessionId(sessionId);

        // Save user message
        conversationRepository.save(Conversation.builder()
                .sessionId(sessionId)
                .role(Conversation.Role.USER)
                .content(userMessage)
                .build());

        String response = null;

        // 1. First attempt: Use configured Spring AI LLM if available
        try {
            ChatClient chatClient = chatClientBuilder.build();
            response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userMessage)
                    .tools("searchProducts", "getProductDetails", "addToCart",
                            "removeFromCart", "viewCart", "initiateCheckout",
                            "getUpsellRecommendations", "listCategories", "checkPaymentStatus",
                            "listCoupons", "applyCoupon", "removeCoupon")
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("Spring AI LLM unavailable. Using Dynamic Autonomous Agent Engine.");
        }

        // 2. Fallback: Multi-Agent Orchestrator (Sales / Support / Checkout)
        if (response == null || response.isBlank()) {
            response = agentOrchestrator.route(sessionId, userMessage);
        }

        // Save assistant response
        conversationRepository.save(Conversation.builder()
                .sessionId(sessionId)
                .role(Conversation.Role.ASSISTANT)
                .content(response)
                .build());

        return response;
    }



    public List<Conversation> getConversationHistory(String sessionId) {
        return conversationRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    public List<String> getAllSessions() {
        return conversationRepository.findAllSessionIds();
    }
}
