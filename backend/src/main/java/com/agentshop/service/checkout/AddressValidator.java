package com.agentshop.service.checkout;

import org.springframework.stereotype.Component;

@Component
public class AddressValidator implements CheckoutValidator {
    @Override
    public ValidationResult validate(CheckoutContext context) {
        String addr = context.deliveryAddress();
        if (addr == null || addr.trim().isEmpty() || addr.equalsIgnoreCase("unknown")) {
            return ValidationResult.failed("Before I can create your checkout link, could you please provide your delivery address?");
        }
        return ValidationResult.success();
    }

    @Override
    public int order() {
        return 1;
    }
}
