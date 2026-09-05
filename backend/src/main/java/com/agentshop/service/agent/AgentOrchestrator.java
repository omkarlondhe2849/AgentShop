package com.agentshop.service.agent;

import com.agentshop.service.AuditService;
import com.agentshop.model.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Agent Orchestrator — Central dispatch point for multi-agent routing.
 *
 * Replaces the monolithic IntentRouter with intelligent classification
 * that routes user messages to one of 3 specialized agents:
 *   1. CheckoutAgent — cart management, payment flow
 *   2. SupportAgent — FAQ, policies, RAG-powered answers
 *   3. SalesAgent — product search, upsell, coupons (default fallback)
 *
 * Intent classification is done locally with lightweight regex/keyword
 * matching — NO extra LLM call needed (token optimization).
 */
@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final SalesAgent salesAgent;
    private final SupportAgent supportAgent;
    private final CheckoutAgent checkoutAgent;
    private final AuditService auditService;

    public enum AgentType { SALES, SUPPORT, CHECKOUT }

    public AgentOrchestrator(SalesAgent salesAgent,
                              SupportAgent supportAgent,
                              CheckoutAgent checkoutAgent,
                              AuditService auditService) {
        this.salesAgent = salesAgent;
        this.supportAgent = supportAgent;
        this.checkoutAgent = checkoutAgent;
        this.auditService = auditService;
    }

    /**
     * Route a user message to the appropriate specialized agent.
     * Classification order (priority):
     *   1. Checkout — highest priority (money actions must be handled strictly)
     *   2. Support — FAQ/policy questions (RAG-backed)
     *   3. Sales — product discovery (default fallback)
     */
    public String route(String sessionId, String message) {
        String lowerMsg = message.toLowerCase().trim();

        AgentType selectedAgent;
        String response;

        // 1. Checkout Agent (highest priority — money actions)
        if (checkoutAgent.canHandle(lowerMsg)) {
            selectedAgent = AgentType.CHECKOUT;
            log.info("🔀 Orchestrator → Checkout Agent for: '{}'", truncate(message));
            response = checkoutAgent.handle(sessionId, message);
        }
        // 2. Support Agent (RAG-powered)
        else if (supportAgent.canHandle(lowerMsg)) {
            selectedAgent = AgentType.SUPPORT;
            log.info("🔀 Orchestrator → Support Agent (RAG) for: '{}'", truncate(message));
            response = supportAgent.handle(sessionId, message);
        }
        // 3. Sales Agent (default)
        else {
            selectedAgent = AgentType.SALES;
            log.info("🔀 Orchestrator → Sales Agent for: '{}'", truncate(message));
            response = salesAgent.handle(sessionId, message);
        }

        // Log which agent handled the request (audit trail)
        try {
            auditService.logAction(
                sessionId,
                AuditLog.ActionType.PRODUCT_SEARCH, // Reuse existing enum
                "Agent Routing: " + selectedAgent.name() + " handled query",
                "Orchestrator classified intent as " + selectedAgent.name(),
                message,
                truncate(response),
                AuditLog.ActionStatus.SUCCESS,
                true,
                "Agent: " + selectedAgent.name()
            );
        } catch (Exception e) {
            log.debug("Audit log for routing skipped: {}", e.getMessage());
        }

        return response;
    }

    private String truncate(String text) {
        if (text == null) return "";
        return text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
