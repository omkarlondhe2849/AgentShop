package com.agentshop.service.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG Augmenter — takes retrieved knowledge entries and constructs
 * a well-formatted response. Works fully offline (no LLM needed)
 * but provides context-augmented prompts when LLM is available.
 */
@Service
public class RagAugmenter {

    private static final Logger log = LoggerFactory.getLogger(RagAugmenter.class);
    private final KnowledgeBaseService knowledgeBaseService;

    public RagAugmenter(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * Generate a formatted answer from retrieved knowledge entries.
     * This is the offline/fallback path that doesn't need the LLM.
     */
    public String generateAnswer(String query) {
        List<KnowledgeBaseService.KnowledgeEntry> results = knowledgeBaseService.retrieve(query, 3);

        if (results.isEmpty()) {
            return null; // Signal that RAG couldn't help
        }

        log.info("📚 RAG retrieved {} entries for query: '{}'", results.size(), query);

        StringBuilder sb = new StringBuilder();

        // Primary answer from the top result
        KnowledgeBaseService.KnowledgeEntry top = results.get(0);
        sb.append("📋 **").append(top.title).append("**\n\n");
        sb.append(top.content).append("\n");

        // If there are additional relevant entries, add them as supplementary info
        if (results.size() > 1) {
            sb.append("\n---\n📌 **Related Information:**\n");
            for (int i = 1; i < results.size(); i++) {
                KnowledgeBaseService.KnowledgeEntry entry = results.get(i);
                sb.append("\n**").append(entry.title).append(":** ");
                // Trim to first 2 sentences for brevity
                String content = entry.content;
                int secondPeriod = content.indexOf('.', content.indexOf('.') + 1);
                if (secondPeriod > 0 && secondPeriod < content.length() - 1) {
                    content = content.substring(0, secondPeriod + 1);
                }
                sb.append(content).append("\n");
            }
        }

        sb.append("\n💡 _Need more help? Just ask me anything!_");
        return sb.toString();
    }

    /**
     * Build an LLM-augmented prompt with retrieved context.
     * This is used when the LLM is available to generate a more natural response.
     * Token-optimized: only includes the most relevant context.
     */
    public String buildAugmentedPrompt(String query) {
        List<KnowledgeBaseService.KnowledgeEntry> results = knowledgeBaseService.retrieve(query, 2);

        if (results.isEmpty()) return null;

        String context = results.stream()
                .map(e -> "### " + e.title + "\n" + e.content)
                .collect(Collectors.joining("\n\n"));

        return String.format("""
                Answer the customer's question using ONLY the context below. Be concise and helpful.
                If the context doesn't contain the answer, say you don't have that information.
                
                CONTEXT:
                %s
                
                CUSTOMER QUESTION: %s
                """, context, query);
    }
}
