package com.agentshop.service.intent;

import com.agentshop.agent.ShoppingTools;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CouponIntentHandler implements IntentHandler {

    private final ShoppingTools shoppingTools;

    public CouponIntentHandler(ShoppingTools shoppingTools) {
        this.shoppingTools = shoppingTools;
    }

    @Override
    public boolean canHandle(String message) {
        return message.contains("coupon") || 
               message.contains("offer") || 
               message.contains("discount") || 
               message.contains("appy") || 
               message.contains("apply");
    }

    @Override
    public int priority() {
        return 90; // High priority so it triggers before generic search
    }

    @Override
    public String handle(String sessionId, String message) {
        shoppingTools.setCurrentSessionId(sessionId);

        // Try to extract a coupon code: e.g. "apply WELCOME10" or "appy WELCOME10"
        Pattern p = Pattern.compile("(?:apply|appy|use(?: coupon)?)\\s+([A-Za-z0-9_]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(message);
        if (m.find()) {
            String code = m.group(1).trim().toUpperCase();
            return shoppingTools.applyCoupon(code);
        }

        if (message.contains("list") || message.contains("show") || message.contains("what") || message.matches(".*(discount|offer|coupon)s?.*")) {
            return shoppingTools.listCoupons();
        }

        if (message.contains("remove") || message.contains("clear")) {
            return shoppingTools.removeCoupon();
        }

        return "Could you specify the coupon code you want to apply? Or ask me to 'list coupons' to see available offers.";
    }
}
