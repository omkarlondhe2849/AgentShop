package com.agentshop.service.checkout;

import com.agentshop.model.CartItem;
import java.math.BigDecimal;
import java.util.List;

public record CheckoutContext(
        String sessionId,
        String customerName,
        String customerEmail,
        String customerPhone,
        String deliveryAddress,
        List<CartItem> cartItems,
        BigDecimal totalAmount,
        BigDecimal discountAmount
) {}
