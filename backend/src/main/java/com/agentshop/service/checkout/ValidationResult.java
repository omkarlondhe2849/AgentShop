package com.agentshop.service.checkout;

public record ValidationResult(boolean passed, String reason) {
    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }
    public static ValidationResult failed(String reason) {
        return new ValidationResult(false, reason);
    }
}
