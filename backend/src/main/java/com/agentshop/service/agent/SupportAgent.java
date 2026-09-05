package com.agentshop.service.agent;

import com.agentshop.service.rag.KnowledgeBaseService;
import com.agentshop.service.rag.RagAugmenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Support Agent — Specialized agent for customer support queries.
 * Powered by RAG (Retrieval-Augmented Generation) over a curated knowledge base.
 *
 * Uses BM25 retrieval to find the most relevant FAQ/policy documents,
 * then formats them into a clear, helpful response.
 */
@Service
public class SupportAgent {

    private static final Logger log = LoggerFactory.getLogger(SupportAgent.class);

    private final KnowledgeBaseService knowledgeBaseService;
    private final RagAugmenter ragAugmenter;

    public SupportAgent(KnowledgeBaseService knowledgeBaseService, RagAugmenter ragAugmenter) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.ragAugmenter = ragAugmenter;
    }

    /**
     * Check if this agent can handle the given message.
     * Returns true if the message appears to be a support/FAQ question.
     */
    public boolean canHandle(String message) {
        String lowerMsg = message.toLowerCase();

        // Explicitly exclude obvious product searches from Support Agent
        if (lowerMsg.matches(".*\\b(show me|looking for|under|budget|rs|₹|buy|purchase|price|cheap)\\b.*")) {
            return false;
        }

        // Explicit support triggers
        if (lowerMsg.matches(".*(return|refund|exchange|warranty|shipping|delivery|track|cancel|payment method|razorpay|privacy|contact|support|help me|how do i|how to|what is your|policy|complaint|issue|problem|account|login|sign up|authentic|genuine|bulk order).*")) {
            return true;
        }

        // Check RAG knowledge base for relevance
        return knowledgeBaseService.isLikelySupportQuery(message);
    }

    /**
     * Handle a support query using RAG.
     */
    public String handle(String sessionId, String message) {
        log.info("🛟 Support Agent handling query: '{}'", message);

        String answer = ragAugmenter.generateAnswer(message);

        if (answer != null) {
            return "🛟 **Support Agent** (powered by RAG)\n\n" + answer;
        }

        // Fallback if RAG couldn't find relevant content
        return "🛟 **Support Agent**\n\n" +
               "I don't have specific information about that topic. Here's what I can help with:\n\n" +
               "  📦 **Returns & Refunds** — ask about our return policy\n" +
               "  🚚 **Shipping & Delivery** — delivery times, tracking\n" +
               "  🔒 **Payment & Security** — payment methods, Razorpay\n" +
               "  🛡️ **Warranty** — product warranty information\n" +
               "  📋 **Order Issues** — cancellation, tracking\n\n" +
               "💡 _Just ask me a specific question and I'll find the answer!_";
    }
}
