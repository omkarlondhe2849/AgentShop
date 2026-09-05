package com.agentshop.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(length = 2000)
    private String itemsSummary; // JSON string of items

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(columnDefinition = "TEXT")
    private String deliveryAddress;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpayPaymentLinkId;

    private String razorpayPaymentLinkUrl;

    private String customerName;

    private String customerEmail;

    private String customerPhone;

    private Integer retryCount;

    private LocalDateTime createdAt;

    private LocalDateTime paidAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (retryCount == null) retryCount = 0;
        if (status == null) status = OrderStatus.CREATED;
    }

    public enum OrderStatus {
        CREATED, PAYMENT_LINK_SENT, ATTEMPTED, PAID, FAILED, REFUNDED, EXPIRED;

        private static final java.util.Map<OrderStatus, java.util.Set<OrderStatus>> TRANSITIONS = java.util.Map.of(
            CREATED,           java.util.Set.of(PAYMENT_LINK_SENT, FAILED),
            PAYMENT_LINK_SENT, java.util.Set.of(PAID, FAILED, EXPIRED, ATTEMPTED),
            ATTEMPTED,         java.util.Set.of(PAID, FAILED),
            FAILED,            java.util.Set.of(PAYMENT_LINK_SENT, ATTEMPTED),
            PAID,              java.util.Set.of(REFUNDED)
        );

        public boolean canTransitionTo(OrderStatus next) {
            return TRANSITIONS.getOrDefault(this, java.util.Collections.emptySet()).contains(next);
        }
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getItemsSummary() {
        return this.itemsSummary;
    }

    public void setItemsSummary(String itemsSummary) {
        this.itemsSummary = itemsSummary;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;
    }

    public OrderStatus getStatus() {
        return this.status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getDeliveryAddress() {
        return this.deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getRazorpayOrderId() {
        return this.razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getRazorpayPaymentId() {
        return this.razorpayPaymentId;
    }

    public void setRazorpayPaymentId(String razorpayPaymentId) {
        this.razorpayPaymentId = razorpayPaymentId;
    }

    public String getRazorpayPaymentLinkId() {
        return this.razorpayPaymentLinkId;
    }

    public void setRazorpayPaymentLinkId(String razorpayPaymentLinkId) {
        this.razorpayPaymentLinkId = razorpayPaymentLinkId;
    }

    public String getRazorpayPaymentLinkUrl() {
        return this.razorpayPaymentLinkUrl;
    }

    public void setRazorpayPaymentLinkUrl(String razorpayPaymentLinkUrl) {
        this.razorpayPaymentLinkUrl = razorpayPaymentLinkUrl;
    }

    public String getCustomerName() {
        return this.customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerEmail() {
        return this.customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerPhone() {
        return this.customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public Integer getRetryCount() {
        return this.retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPaidAt() {
        return this.paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public Order() {}

    public Order(Long id, String sessionId, String itemsSummary, BigDecimal totalAmount, OrderStatus status, String razorpayOrderId, String razorpayPaymentId, String razorpayPaymentLinkId, String razorpayPaymentLinkUrl, String customerName, String customerEmail, String customerPhone, Integer retryCount, LocalDateTime createdAt, LocalDateTime paidAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.itemsSummary = itemsSummary;
        this.totalAmount = totalAmount;
        this.status = status;
        this.razorpayOrderId = razorpayOrderId;
        this.razorpayPaymentId = razorpayPaymentId;
        this.razorpayPaymentLinkId = razorpayPaymentLinkId;
        this.razorpayPaymentLinkUrl = razorpayPaymentLinkUrl;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.customerPhone = customerPhone;
        this.retryCount = retryCount;
        this.createdAt = createdAt;
        this.paidAt = paidAt;
    }

    public static OrderBuilder builder() {
        return new OrderBuilder();
    }

    public static class OrderBuilder {
        private Long id;
        private String sessionId;
        private String itemsSummary;
        private BigDecimal totalAmount;
        private OrderStatus status;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private String razorpayPaymentLinkId;
        private String razorpayPaymentLinkUrl;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private Integer retryCount;
        private LocalDateTime createdAt;
        private LocalDateTime paidAt;

        public OrderBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrderBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public OrderBuilder itemsSummary(String itemsSummary) {
            this.itemsSummary = itemsSummary;
            return this;
        }

        public OrderBuilder totalAmount(BigDecimal totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }

        public OrderBuilder status(OrderStatus status) {
            this.status = status;
            return this;
        }

        public OrderBuilder razorpayOrderId(String razorpayOrderId) {
            this.razorpayOrderId = razorpayOrderId;
            return this;
        }

        public OrderBuilder razorpayPaymentId(String razorpayPaymentId) {
            this.razorpayPaymentId = razorpayPaymentId;
            return this;
        }

        public OrderBuilder razorpayPaymentLinkId(String razorpayPaymentLinkId) {
            this.razorpayPaymentLinkId = razorpayPaymentLinkId;
            return this;
        }

        public OrderBuilder razorpayPaymentLinkUrl(String razorpayPaymentLinkUrl) {
            this.razorpayPaymentLinkUrl = razorpayPaymentLinkUrl;
            return this;
        }

        public OrderBuilder customerName(String customerName) {
            this.customerName = customerName;
            return this;
        }

        public OrderBuilder customerEmail(String customerEmail) {
            this.customerEmail = customerEmail;
            return this;
        }

        public OrderBuilder customerPhone(String customerPhone) {
            this.customerPhone = customerPhone;
            return this;
        }

        public OrderBuilder retryCount(Integer retryCount) {
            this.retryCount = retryCount;
            return this;
        }

        public OrderBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public OrderBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }

        public Order build() {
            return new Order(this.id, this.sessionId, this.itemsSummary, this.totalAmount, this.status, this.razorpayOrderId, this.razorpayPaymentId, this.razorpayPaymentLinkId, this.razorpayPaymentLinkUrl, this.customerName, this.customerEmail, this.customerPhone, this.retryCount, this.createdAt, this.paidAt);
        }
    }
}
