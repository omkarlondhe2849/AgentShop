package com.agentshop.service.checkout;

public interface CheckoutValidator {
    ValidationResult validate(CheckoutContext context);
    int order();
}
