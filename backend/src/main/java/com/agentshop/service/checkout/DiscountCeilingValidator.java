package com.agentshop.service.checkout;

import com.agentshop.event.ShoppingEvents.CheckoutBlockedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Gating validator: enforces a merchant-configured maximum discount percentage.
 * 
 * The LLM can *request* a discount via the coupon tool, but this validator
 * ensures no checkout ever proceeds with a discount exceeding the ceiling.
 * This is the critical difference between logging a mistake and preventing one.
 */
@Component
public class DiscountCeilingValidator implements CheckoutValidator {

    private static final Logger log = LoggerFactory.getLogger(DiscountCeilingValidator.class);

    private final double maxDiscountPercent;
    private final ApplicationEventPublisher eventPublisher;

    public DiscountCeilingValidator(
            @Value("${agentshop.max-discount-percent:30}") double maxDiscountPercent,
            ApplicationEventPublisher eventPublisher) {
        this.maxDiscountPercent = maxDiscountPercent;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ValidationResult validate(CheckoutContext context) {
        if (context.discountAmount() == null || context.discountAmount().compareTo(BigDecimal.ZERO) == 0) {
            return ValidationResult.success();
        }

        // Calculate original cart total (before discount)
        BigDecimal originalTotal = context.totalAmount().add(context.discountAmount());
        if (originalTotal.compareTo(BigDecimal.ZERO) == 0) {
            return ValidationResult.success();
        }

        double actualPercent = context.discountAmount()
                .divide(originalTotal, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();

        if (actualPercent > maxDiscountPercent) {
            log.warn("🚫 GATED: Discount {}% exceeds merchant ceiling of {}% for session {}",
                    String.format("%.1f", actualPercent),
                    String.format("%.0f", maxDiscountPercent),
                    context.sessionId());

            eventPublisher.publishEvent(new CheckoutBlockedEvent(
                    context.sessionId(), context.totalAmount(), (long) maxDiscountPercent));

            return ValidationResult.failed(
                    String.format("⚠️ **Discount Blocked** — The applied discount of %.1f%% exceeds the maximum allowed " +
                            "discount of %.0f%%. This is a merchant-configured safety ceiling. " +
                            "Please remove or change your coupon to proceed.",
                            actualPercent, maxDiscountPercent));
        }

        log.info("✅ Discount gate passed: {}% (ceiling: {}%)",
                String.format("%.1f", actualPercent), String.format("%.0f", maxDiscountPercent));
        return ValidationResult.success();
    }

    @Override
    public int order() {
        return 2; // Run before MaxAmountValidator but after CartNotEmpty
    }
}
