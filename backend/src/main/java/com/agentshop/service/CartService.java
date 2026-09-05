package com.agentshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agentshop.model.CartItem;
import com.agentshop.model.Product;
import com.agentshop.repository.CartRepository;
import com.agentshop.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class CartService {

    public CartService(CartRepository cartRepository, ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    private static final Logger log = LoggerFactory.getLogger(CartService.class);


    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartItem addToCart(String sessionId, Long productId, int quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

        Optional<CartItem> existing = cartRepository.findBySessionIdAndProductId(sessionId, productId);

        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + quantity);
            return cartRepository.save(item);
        }

        CartItem newItem = CartItem.builder()
                .sessionId(sessionId)
                .product(product)
                .quantity(quantity)
                .build();

        return cartRepository.save(newItem);
    }

    @Transactional
    public void removeFromCart(String sessionId, Long productId) {
        cartRepository.deleteBySessionIdAndProductId(sessionId, productId);
    }

    public List<CartItem> getCart(String sessionId) {
        return cartRepository.findBySessionId(sessionId);
    }

    public BigDecimal getCartTotal(String sessionId) {
        return cartRepository.findBySessionId(sessionId).stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getCartSummary(String sessionId) {
        List<CartItem> items = getCart(sessionId);
        if (items.isEmpty()) return "Cart is empty.";

        StringBuilder sb = new StringBuilder("🛒 Cart Summary:\n");
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items) {
            BigDecimal lineTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(lineTotal);
            sb.append(String.format("  • %s (x%d) — ₹%s\n",
                    item.getProduct().getName(), item.getQuantity(), lineTotal.toPlainString()));
        }
        sb.append(String.format("\n💰 Total: ₹%s", total.toPlainString()));
        return sb.toString();
    }

    @Transactional
    public void clearCart(String sessionId) {
        cartRepository.deleteBySessionId(sessionId);
    }
}
