package com.agentshop.service.checkout;

import org.springframework.stereotype.Component;

@Component
public class CartNotEmptyValidator implements CheckoutValidator {
    @Override
    public ValidationResult validate(CheckoutContext context) {
        if (context.cartItems().isEmpty()) {
            return ValidationResult.failed("Your cart is empty. Please add some products first.");
        }
        return ValidationResult.success();
    }

    @Override
    public int order() {
        return 2;
    }
}
