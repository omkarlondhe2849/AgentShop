package com.agentshop.service.checkout;

import com.agentshop.event.ShoppingEvents.CheckoutBlockedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class MaxAmountValidator implements CheckoutValidator {
    
    private final long maxOrderAmountPaise;
    private final ApplicationEventPublisher eventPublisher;

    public MaxAmountValidator(@Value("${agentshop.max-order-amount:5000000}") long maxOrderAmountPaise, 
                              ApplicationEventPublisher eventPublisher) {
        this.maxOrderAmountPaise = maxOrderAmountPaise;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ValidationResult validate(CheckoutContext context) {
        long totalPaise = context.totalAmount().multiply(BigDecimal.valueOf(100)).longValue();
        if (totalPaise > maxOrderAmountPaise) {
            eventPublisher.publishEvent(new CheckoutBlockedEvent(context.sessionId(), context.totalAmount(), maxOrderAmountPaise));
            return ValidationResult.failed("⚠️ Sorry, your order total of ₹" + context.totalAmount().toPlainString() + 
                    " exceeds our maximum single order limit of ₹" + (maxOrderAmountPaise / 100) + 
                    ". Please remove some items to proceed.");
        }
        return ValidationResult.success();
    }

    @Override
    public int order() {
        return 3;
    }
}
