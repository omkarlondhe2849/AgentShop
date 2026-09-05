package com.agentshop.service.rag;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * In-memory BM25-based knowledge base retrieval engine for RAG.
 * Loads curated FAQ/policy documents at startup and provides
 * fast semantic retrieval without requiring an external vector DB.
 */
@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final ObjectMapper objectMapper;
    private List<KnowledgeEntry> entries = new ArrayList<>();

    // Inverted index: keyword -> list of entry indices
    private Map<String, List<Integer>> invertedIndex = new HashMap<>();

    // IDF values for BM25
    private Map<String, Double> idfScores = new HashMap<>();

    // BM25 parameters
    private static final double K1 = 1.5;
    private static final double B = 0.75;
    private double avgDocLength = 0;

    public KnowledgeBaseService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KnowledgeEntry {
        public String id;
        public String title;
        public String category;
        public List<String> keywords;
        public String content;

        public String getAllText() {
            StringBuilder sb = new StringBuilder();
            sb.append(title).append(" ").append(content);
            if (keywords != null) {
                sb.append(" ").append(String.join(" ", keywords));
            }
            return sb.toString().toLowerCase();
        }
    }

    @PostConstruct
    public void init() {
        try {
            InputStream is = new ClassPathResource("knowledge_base.json").getInputStream();
            entries = objectMapper.readValue(is, new TypeReference<List<KnowledgeEntry>>() {});
            buildIndex();
            log.info("📚 RAG Knowledge Base loaded: {} entries, {} unique terms", entries.size(), invertedIndex.size());
        } catch (Exception e) {
            log.error("Failed to load knowledge base", e);
        }
    }

    private void buildIndex() {
        double totalLength = 0;

        for (int i = 0; i < entries.size(); i++) {
            String text = entries.get(i).getAllText();
            String[] tokens = tokenize(text);
            totalLength += tokens.length;

            Set<String> seen = new HashSet<>();
            for (String token : tokens) {
                invertedIndex.computeIfAbsent(token, k -> new ArrayList<>());
                if (!seen.contains(token)) {
                    invertedIndex.get(token).add(i);
                    seen.add(token);
                }
            }
        }

        avgDocLength = entries.isEmpty() ? 1 : totalLength / entries.size();

        // Compute IDF for each term
        int N = entries.size();
        for (Map.Entry<String, List<Integer>> entry : invertedIndex.entrySet()) {
            int df = entry.getValue().size();
            double idf = Math.log((N - df + 0.5) / (df + 0.5) + 1.0);
            idfScores.put(entry.getKey(), idf);
        }
    }

    /**
     * Retrieve the top-K most relevant knowledge entries for a query using BM25 scoring.
     */
    public List<KnowledgeEntry> retrieve(String query, int topK) {
        if (entries.isEmpty() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String[] queryTokens = tokenize(query.toLowerCase());
        double[] scores = new double[entries.size()];

        for (String qToken : queryTokens) {
            Double idf = idfScores.get(qToken);
            if (idf == null) continue;

            List<Integer> postings = invertedIndex.getOrDefault(qToken, Collections.emptyList());
            for (int docIdx : postings) {
                String docText = entries.get(docIdx).getAllText();
                String[] docTokens = tokenize(docText);
                int tf = countOccurrences(docTokens, qToken);
                double docLen = docTokens.length;

                // BM25 score
                double numerator = tf * (K1 + 1);
                double denominator = tf + K1 * (1 - B + B * (docLen / avgDocLength));
                scores[docIdx] += idf * (numerator / denominator);
            }
        }

        // Also boost entries whose explicit keywords match
        for (int i = 0; i < entries.size(); i++) {
            KnowledgeEntry entry = entries.get(i);
            if (entry.keywords != null) {
                for (String keyword : entry.keywords) {
                    if (query.toLowerCase().contains(keyword.toLowerCase())) {
                        scores[i] += 2.0; // Keyword match bonus
                    }
                }
            }
        }

        // Sort by score descending and return top-K
        List<Map.Entry<Integer, Double>> ranked = new ArrayList<>();
        for (int i = 0; i < scores.length; i++) {
            if (scores[i] > 0) {
                ranked.add(Map.entry(i, scores[i]));
            }
        }
        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        return ranked.stream()
                .limit(topK)
                .map(e -> entries.get(e.getKey()))
                .collect(Collectors.toList());
    }

    /**
     * Check if a query is likely a support/FAQ question (vs a product search).
     */
    public boolean isLikelySupportQuery(String query) {
        List<KnowledgeEntry> results = retrieve(query, 1);
        if (results.isEmpty()) return false;
        // If the top result has a high enough score, it's likely a support question
        String[] queryTokens = tokenize(query.toLowerCase());
        String docText = results.get(0).getAllText();
        String[] docTokens = tokenize(docText);

        int matchCount = 0;
        for (String qt : queryTokens) {
            for (String keyword : results.get(0).keywords) {
                if (keyword.toLowerCase().contains(qt) || qt.contains(keyword.toLowerCase())) {
                    matchCount++;
                    break;
                }
            }
        }
        return matchCount >= 1;
    }

    private String[] tokenize(String text) {
        return text.replaceAll("[^a-zA-Z0-9₹]", " ")
                   .toLowerCase()
                   .split("\\s+");
    }

    private int countOccurrences(String[] tokens, String target) {
        int count = 0;
        for (String t : tokens) {
            if (t.equals(target)) count++;
        }
        return count;
    }
}
