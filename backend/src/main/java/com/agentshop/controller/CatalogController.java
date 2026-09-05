package com.agentshop.controller;

import com.agentshop.model.Product;
import com.agentshop.repository.ProductRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

import com.agentshop.service.CouponService;

/**
 * Agent-Readable Catalog API — follows ACP/UCP patterns with structured JSON responses.
 * Designed for both human and AI agent consumption.
 */
@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final ProductRepository productRepository;
    private final CouponService couponService;

    public CatalogController(ProductRepository productRepository, CouponService couponService) {
        this.productRepository = productRepository;
        this.couponService = couponService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getCatalog() {
        List<Product> products = productRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("@context", "https://schema.org");
        response.put("@type", "ItemList");
        response.put("name", "AgentShop Product Catalog");
        response.put("description", "Complete product catalog for AI agents and buyers");
        response.put("numberOfItems", products.size());
        response.put("itemListElement", products.stream().map(this::toSchemaProduct).toList());
        response.put("_links", Map.of(
                "self", Map.of("href", "/api/v1/catalog"),
                "search", Map.of("href", "/api/v1/catalog/search?q={query}&category={category}"),
                "categories", Map.of("href", "/api/v1/catalog/categories"),
                "schema", Map.of("href", "/api/v1/catalog/schema"),
                "capabilities", Map.of("href", "/api/v1/catalog/capabilities")
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchCatalog(
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double maxPrice) {

        List<Product> results;
        if (maxPrice != null) {
            results = productRepository.searchProductsWithMaxPrice(q, BigDecimal.valueOf(maxPrice));
        } else {
            results = productRepository.searchProducts(q);
        }

        if (category != null && !category.isEmpty()) {
            results = results.stream()
                    .filter(p -> p.getCategory().equalsIgnoreCase(category))
                    .toList();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("@context", "https://schema.org");
        response.put("@type", "SearchResultsPage");
        response.put("query", q);
        response.put("numberOfItems", results.size());
        response.put("itemListElement", results.stream().map(this::toSchemaProduct).toList());
        response.put("_actions", List.of(
                Map.of("name", "addToCart", "method", "POST", "href", "/api/v1/cart",
                        "fields", List.of(
                                Map.of("name", "productId", "type", "number", "required", true),
                                Map.of("name", "quantity", "type", "number", "required", false, "default", 1)
                        ))
        ));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getProduct(@PathVariable Long id) {
        return productRepository.findById(id)
                .map(product -> {
                    Map<String, Object> response = toSchemaProduct(product);
                    response.put("_actions", List.of(
                            Map.of("name", "addToCart", "method", "POST", "href", "/api/v1/cart",
                                    "body", Map.of("productId", id, "quantity", 1)),
                            Map.of("name", "getRelated", "method", "GET",
                                    "href", "/api/v1/catalog/search?category=" + product.getCategory())
                    ));
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        List<String> categories = productRepository.findAllCategories();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("@type", "ItemList");
        response.put("categories", categories);
        response.put("_links", categories.stream()
                .map(c -> Map.of("name", c, "href", "/api/v1/catalog/search?category=" + c))
                .toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/capabilities")
    public ResponseEntity<Map<String, Object>> getCapabilities() {
        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("@type", "AgentCapabilities");
        capabilities.put("version", "1.0");
        capabilities.put("merchant", "AgentShop");
        capabilities.put("supportedActions", List.of(
                Map.of("action", "browse", "description", "Browse full product catalog", "endpoint", "GET /api/v1/catalog"),
                Map.of("action", "search", "description", "Search products by query, category, and price range", "endpoint", "GET /api/v1/catalog/search"),
                Map.of("action", "productDetails", "description", "Get detailed product information", "endpoint", "GET /api/v1/catalog/{id}"),
                Map.of("action", "addToCart", "description", "Add a product to cart", "endpoint", "POST /api/v1/cart"),
                Map.of("action", "viewCart", "description", "View current cart", "endpoint", "GET /api/v1/cart/{sessionId}"),
                Map.of("action", "checkout", "description", "Initiate checkout and get payment link", "endpoint", "POST /api/v1/checkout")
        ));
        capabilities.put("paymentMethods", List.of("UPI", "Credit Card", "Debit Card", "Net Banking", "Wallets"));
        capabilities.put("currency", "INR");
        capabilities.put("maxOrderAmount", 50000);
        capabilities.put("auditTrail", true);
        return ResponseEntity.ok(capabilities);
    }

    @GetMapping("/schema")
    public ResponseEntity<Map<String, Object>> getSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("openapi", "3.0.0");
        schema.put("info", Map.of(
                "title", "AgentShop Catalog API",
                "version", "1.0",
                "description", "Machine-readable API for AI agents to browse, search, and purchase products"
        ));
        return ResponseEntity.ok(schema);
    }

    // ---- Helper: Convert product to Schema.org format ----
    private Map<String, Object> toSchemaProduct(Product product) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("@type", "Product");
        schema.put("productID", product.getId());
        schema.put("name", product.getName());
        schema.put("description", product.getDescription());
        schema.put("offers", Map.of(
                "@type", "Offer",
                "price", product.getPrice(),
                "priceCurrency", "INR",
                "availability", product.getStock() > 0 ? "InStock" : "OutOfStock",
                "inventoryLevel", product.getStock()
        ));
        schema.put("category", product.getCategory());
        schema.put("image", product.getImageUrl());
        schema.put("aggregateRating", Map.of(
                "@type", "AggregateRating",
                "ratingValue", product.getRating(),
                "reviewCount", product.getReviewCount()
        ));
        schema.put("keywords", product.getTags());
        return schema;
    }
}
