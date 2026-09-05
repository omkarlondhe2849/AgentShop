package com.agentshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agentshop.model.AuditLog;
import com.agentshop.model.Order;
import com.razorpay.*;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class RazorpayService {
    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);


    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    private RazorpayClient razorpayClient;

    private final AuditService auditService;

    public RazorpayService(AuditService auditService) {
        this.auditService = auditService;
    }

    @PostConstruct
    public void init() {
        try {
            if (!keyId.contains("placeholder")) {
                razorpayClient = new RazorpayClient(keyId, keySecret);
                log.info("✅ Razorpay client initialized with key: {}...", keyId.substring(0, Math.min(keyId.length(), 15)));
            } else {
                log.warn("⚠️ Razorpay running in MOCK mode. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET env vars for real integration.");
            }
        } catch (RazorpayException e) {
            log.error("Failed to initialize Razorpay client", e);
        }
    }

    /**
     * Creates a Razorpay Order
     */
    public JSONObject createOrder(String sessionId, int amountInPaise, String currency, String receipt) {
        if (razorpayClient == null) {
            return createMockOrder(amountInPaise, currency, receipt);
        }

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", receipt);

            com.razorpay.Order rzpOrder = razorpayClient.orders.create(orderRequest);
            JSONObject result = new JSONObject(rzpOrder.toString());

            log.info("Razorpay Order created: {}", result.getString("id"));
            return result;
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order", e);
            throw new RuntimeException("Payment order creation failed: " + e.getMessage());
        }
    }

    /**
     * Creates a Razorpay Payment Link
     */
    public JSONObject createPaymentLink(String sessionId, int amountInPaise, String customerName,
                                         String customerEmail, String customerPhone,
                                         String description, String orderId) {
        if (razorpayClient == null) {
            return createMockPaymentLink(amountInPaise, customerName, description);
        }

        try {
            JSONObject linkRequest = new JSONObject();
            linkRequest.put("amount", amountInPaise);
            linkRequest.put("currency", "INR");
            linkRequest.put("description", description);
            linkRequest.put("reference_id", orderId);

            JSONObject customer = new JSONObject();
            customer.put("name", customerName);
            customer.put("email", customerEmail);
            customer.put("contact", customerPhone);
            linkRequest.put("customer", customer);

            JSONObject notify = new JSONObject();
            notify.put("sms", false);
            notify.put("email", false);
            linkRequest.put("notify", notify);

            linkRequest.put("callback_url", "http://localhost:5173/payment-status");
            linkRequest.put("callback_method", "get");

            PaymentLink link = razorpayClient.paymentLink.create(linkRequest);
            JSONObject result = new JSONObject(link.toString());

            log.info("Razorpay Payment Link created: {}", result.getString("short_url"));
            return result;
        } catch (RazorpayException e) {
            log.error("Failed to create payment link", e);
            throw new RuntimeException("Payment link creation failed: " + e.getMessage());
        }
    }

    /**
     * Fetch payment details
     */
    public JSONObject fetchPayment(String paymentId) {
        if (razorpayClient == null) {
            JSONObject mock = new JSONObject();
            mock.put("id", paymentId);
            mock.put("status", "captured");
            mock.put("amount", 100000);
            return mock;
        }

        try {
            Payment payment = razorpayClient.payments.fetch(paymentId);
            return new JSONObject(payment.toString());
        } catch (RazorpayException e) {
            log.error("Failed to fetch payment: {}", paymentId, e);
            throw new RuntimeException("Failed to fetch payment: " + e.getMessage());
        }
    }

    /**
     * Verify webhook signature
     */
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            return Utils.verifyWebhookSignature(payload, signature, webhookSecret);
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed", e);
            return false;
        }
    }

    // ---- Mock methods for when Razorpay keys aren't configured ----

    private JSONObject createMockOrder(int amountInPaise, String currency, String receipt) {
        JSONObject mock = new JSONObject();
        mock.put("id", "order_mock_" + System.currentTimeMillis());
        mock.put("entity", "order");
        mock.put("amount", amountInPaise);
        mock.put("amount_paid", 0);
        mock.put("currency", currency);
        mock.put("receipt", receipt);
        mock.put("status", "created");
        log.info("MOCK Razorpay Order created: {}", mock.getString("id"));
        return mock;
    }

    private JSONObject createMockPaymentLink(int amountInPaise, String customerName, String description) {
        String linkId = "plink_mock_" + System.currentTimeMillis();
        JSONObject mock = new JSONObject();
        mock.put("id", linkId);
        mock.put("amount", amountInPaise);
        mock.put("currency", "INR");
        mock.put("description", description);
        mock.put("short_url", "https://rzp.io/i/mock-" + linkId);
        mock.put("status", "created");
        log.info("MOCK Razorpay Payment Link created: {}", mock.getString("short_url"));
        return mock;
    }
}
