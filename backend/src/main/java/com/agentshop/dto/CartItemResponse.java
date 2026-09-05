package com.agentshop.dto;

import com.agentshop.model.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long productId,
        String name,
        int quantity,
        BigDecimal price,
        BigDecimal lineTotal
) {
    public static CartItemResponse from(CartItem item) {
        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getProduct().getPrice(),
                item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))
        );
    }
}
