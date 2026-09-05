package com.agentshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * Coupon & Offers Engine — manages promotional offers that the AI agent
 * can proactively suggest and apply during shopping sessions.
 */
@Service
public class CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponService.class);

    // Active coupons store
    private final List<Coupon> activeCoupons = new ArrayList<>();

    // Track which coupons have been applied per session
    private final Map<String, String> appliedCoupons = new HashMap<>();

    public CouponService() {
        // Seed default promotional offers
        activeCoupons.add(new Coupon("WELCOME10", "Welcome Discount", "10% off on your first order", 
                CouponType.PERCENTAGE, BigDecimal.valueOf(10), BigDecimal.valueOf(500), null, true));
        activeCoupons.add(new Coupon("FLAT200", "Flat ₹200 Off", "₹200 off on orders above ₹2,000", 
                CouponType.FLAT, BigDecimal.valueOf(200), BigDecimal.valueOf(2000), null, true));
        activeCoupons.add(new Coupon("RAZORPAY15", "Razorpay Special", "15% off (max ₹750) on Razorpay checkout", 
                CouponType.PERCENTAGE, BigDecimal.valueOf(15), BigDecimal.valueOf(1000), BigDecimal.valueOf(750), true));
        activeCoupons.add(new Coupon("TECH500", "Tech Lovers Deal", "₹500 off on Electronics & Laptops above ₹5,000", 
                CouponType.FLAT, BigDecimal.valueOf(500), BigDecimal.valueOf(5000), null, true));
        activeCoupons.add(new Coupon("AUDIO20", "Audio Gear Sale", "20% off (max ₹600) on Headphones", 
                CouponType.PERCENTAGE, BigDecimal.valueOf(20), BigDecimal.valueOf(1000), BigDecimal.valueOf(600), true));
        activeCoupons.add(new Coupon("FREESHIP", "Free Shipping", "Free delivery on orders above ₹999", 
                CouponType.FLAT, BigDecimal.valueOf(99), BigDecimal.valueOf(999), null, true));
        activeCoupons.add(new Coupon("MEGA25", "Mega Sale 25%", "25% off (max ₹1,500) — limited time!", 
                CouponType.PERCENTAGE, BigDecimal.valueOf(25), BigDecimal.valueOf(3000), BigDecimal.valueOf(1500), true));
        activeCoupons.add(new Coupon("NEWUSER50", "New User Bonus", "₹50 off — no minimum order", 
                CouponType.FLAT, BigDecimal.valueOf(50), BigDecimal.ZERO, null, true));
    }

    /**
     * List all active coupons with formatted descriptions.
     */
    public String listAvailableCoupons() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Coupon> available = activeCoupons.stream().filter(c -> c.active).toList();
            String json = mapper.writeValueAsString(available);
            return "🎫 **Available Offers & Coupons:**\n\n__COUPON_LIST_START__\n" + json + "\n__COUPON_LIST_END__\n\n💡 _Click Apply to use any coupon!_";
        } catch (Exception e) {
            return "Coupons available.";
        }
    }

    /**
     * Get proactive coupon suggestions based on cart total and categories.
     */
    public String getProactiveSuggestions(BigDecimal cartTotal, List<String> cartCategories) {
        List<Coupon> suggestions = new ArrayList<>();

        for (Coupon c : activeCoupons) {
            if (!c.active) continue;
            if (cartTotal.compareTo(c.minOrderAmount) >= 0) {
                suggestions.add(c);
            }
        }

        if (suggestions.isEmpty()) return "";

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = mapper.writeValueAsString(suggestions);
            return "\n---\n🎉 **Offers Available for Your Cart:**\n\n__COUPON_LIST_START__\n" + json + "\n__COUPON_LIST_END__\n\n_Click Apply to use any coupon!_";
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Apply a coupon code to a cart total. Returns result with discount amount.
     */
    public CouponResult applyCoupon(String sessionId, String code, BigDecimal cartTotal) {
        String upperCode = code.toUpperCase().trim();
        log.info("🏷️ Applying coupon: {} for session {} with cart total ₹{}", upperCode, sessionId, cartTotal);

        // Check if already applied
        if (appliedCoupons.containsKey(sessionId)) {
            return new CouponResult(false, BigDecimal.ZERO, cartTotal,
                    "❌ You already have coupon **`" + appliedCoupons.get(sessionId) + "`** applied. Remove it first to use a different one.");
        }

        Coupon coupon = activeCoupons.stream()
                .filter(c -> c.code.equalsIgnoreCase(upperCode) && c.active)
                .findFirst()
                .orElse(null);

        if (coupon == null) {
            return new CouponResult(false, BigDecimal.ZERO, cartTotal,
                    "❌ Coupon code **`" + upperCode + "`** is invalid or expired.");
        }

        if (cartTotal.compareTo(coupon.minOrderAmount) < 0) {
            return new CouponResult(false, BigDecimal.ZERO, cartTotal,
                    String.format("❌ Minimum order of ₹%s required for **`%s`**. Your cart total is ₹%s.",
                            coupon.minOrderAmount.toPlainString(), upperCode, cartTotal.toPlainString()));
        }

        BigDecimal discount = calculateDiscount(coupon, cartTotal);
        BigDecimal newTotal = cartTotal.subtract(discount);

        appliedCoupons.put(sessionId, upperCode);

        String message = String.format(
                "✅ **Coupon `%s` Applied Successfully!**\n\n" +
                "🏷️ %s\n" +
                "💰 Discount: **-₹%s**\n" +
                "🛒 Original Total: ₹%s\n" +
                "✨ **New Total: ₹%s**\n\n" +
                "_Discount will be reflected at checkout._",
                upperCode, coupon.description, discount.toPlainString(),
                cartTotal.toPlainString(), newTotal.toPlainString());

        return new CouponResult(true, discount, newTotal, message);
    }

    /**
     * Get the discount amount for a session's current cart total.
     */
    public BigDecimal getDiscountForSession(String sessionId, BigDecimal cartTotal) {
        String code = appliedCoupons.get(sessionId);
        if (code == null) return BigDecimal.ZERO;
        
        Coupon coupon = activeCoupons.stream()
                .filter(c -> c.code.equalsIgnoreCase(code) && c.active)
                .findFirst()
                .orElse(null);
                
        if (coupon == null || cartTotal.compareTo(coupon.minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        
        return calculateDiscount(coupon, cartTotal);
    }

    /**
     * Remove applied coupon from session.
     */
    public String removeCoupon(String sessionId) {
        String removed = appliedCoupons.remove(sessionId);
        if (removed != null) {
            return "✅ Coupon **`" + removed + "`** has been removed. You can apply a different coupon now.";
        }
        return "ℹ️ No coupon is currently applied.";
    }

    /**
     * Get the currently applied coupon for a session.
     */
    public String getAppliedCoupon(String sessionId) {
        return appliedCoupons.get(sessionId);
    }

    /**
     * Get discount amount for a session's applied coupon.
     */
    public BigDecimal getSessionDiscount(String sessionId, BigDecimal cartTotal) {
        String code = appliedCoupons.get(sessionId);
        if (code == null) return BigDecimal.ZERO;

        Coupon coupon = activeCoupons.stream()
                .filter(c -> c.code.equalsIgnoreCase(code))
                .findFirst()
                .orElse(null);

        if (coupon == null) return BigDecimal.ZERO;
        if (cartTotal.compareTo(coupon.minOrderAmount) < 0) return BigDecimal.ZERO;

        return calculateDiscount(coupon, cartTotal);
    }

    private BigDecimal calculateDiscount(Coupon coupon, BigDecimal cartTotal) {
        BigDecimal discount;
        if (coupon.type == CouponType.PERCENTAGE) {
            discount = cartTotal.multiply(coupon.value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.maxDiscount != null && discount.compareTo(coupon.maxDiscount) > 0) {
                discount = coupon.maxDiscount;
            }
        } else {
            discount = coupon.value;
        }
        // Discount cannot exceed cart total
        if (discount.compareTo(cartTotal) > 0) {
            discount = cartTotal;
        }
        return discount;
    }

    // ── Inner Types ──

    public enum CouponType { PERCENTAGE, FLAT }

    public record Coupon(String code, String title, String description, CouponType type,
                         BigDecimal value, BigDecimal minOrderAmount, BigDecimal maxDiscount, boolean active) {}

    public record CouponResult(boolean applied, BigDecimal discount, BigDecimal newTotal, String message) {}
}
