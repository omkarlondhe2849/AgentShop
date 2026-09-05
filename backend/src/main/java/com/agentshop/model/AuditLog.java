package com.agentshop.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActionType actionType;

    @Column(length = 500)
    private String description;

    @Column(length = 1000)
    private String agentReasoning;

    @Column(length = 2000)
    private String inputData; // JSON

    @Column(length = 2000)
    private String outputData; // JSON

    @Enumerated(EnumType.STRING)
    private ActionStatus status;

    private Boolean boundaryCheckPassed;

    private String boundaryDetails;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }

    public enum ActionType {
        PRODUCT_SEARCH,
        PRODUCT_VIEW,
        ADD_TO_CART,
        REMOVE_FROM_CART,
        CART_VIEW,
        CHECKOUT_INITIATED,
        PAYMENT_LINK_CREATED,
        PAYMENT_SUCCESS,
        PAYMENT_FAILED,
        PAYMENT_RETRY,
        UPSELL_OFFERED,
        UPSELL_ACCEPTED,
        UPSELL_DECLINED,
        ORDER_AMOUNT_CHECK,
        CONVERSATION_START,
        AGENT_ERROR
    }

    public enum ActionStatus {
        SUCCESS, FAILURE, PENDING, BLOCKED
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

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public ActionType getActionType() {
        return this.actionType;
    }

    public void setActionType(ActionType actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAgentReasoning() {
        return this.agentReasoning;
    }

    public void setAgentReasoning(String agentReasoning) {
        this.agentReasoning = agentReasoning;
    }

    public String getInputData() {
        return this.inputData;
    }

    public void setInputData(String inputData) {
        this.inputData = inputData;
    }

    public String getOutputData() {
        return this.outputData;
    }

    public void setOutputData(String outputData) {
        this.outputData = outputData;
    }

    public ActionStatus getStatus() {
        return this.status;
    }

    public void setStatus(ActionStatus status) {
        this.status = status;
    }

    public Boolean getBoundaryCheckPassed() {
        return this.boundaryCheckPassed;
    }

    public void setBoundaryCheckPassed(Boolean boundaryCheckPassed) {
        this.boundaryCheckPassed = boundaryCheckPassed;
    }

    public String getBoundaryDetails() {
        return this.boundaryDetails;
    }

    public void setBoundaryDetails(String boundaryDetails) {
        this.boundaryDetails = boundaryDetails;
    }

    public AuditLog() {}

    public AuditLog(Long id, String sessionId, LocalDateTime timestamp, ActionType actionType, String description, String agentReasoning, String inputData, String outputData, ActionStatus status, Boolean boundaryCheckPassed, String boundaryDetails) {
        this.id = id;
        this.sessionId = sessionId;
        this.timestamp = timestamp;
        this.actionType = actionType;
        this.description = description;
        this.agentReasoning = agentReasoning;
        this.inputData = inputData;
        this.outputData = outputData;
        this.status = status;
        this.boundaryCheckPassed = boundaryCheckPassed;
        this.boundaryDetails = boundaryDetails;
    }

    public static AuditLogBuilder builder() {
        return new AuditLogBuilder();
    }

    public static class AuditLogBuilder {
        private Long id;
        private String sessionId;
        private LocalDateTime timestamp;
        private ActionType actionType;
        private String description;
        private String agentReasoning;
        private String inputData;
        private String outputData;
        private ActionStatus status;
        private Boolean boundaryCheckPassed;
        private String boundaryDetails;

        public AuditLogBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuditLogBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public AuditLogBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public AuditLogBuilder actionType(ActionType actionType) {
            this.actionType = actionType;
            return this;
        }

        public AuditLogBuilder description(String description) {
            this.description = description;
            return this;
        }

        public AuditLogBuilder agentReasoning(String agentReasoning) {
            this.agentReasoning = agentReasoning;
            return this;
        }

        public AuditLogBuilder inputData(String inputData) {
            this.inputData = inputData;
            return this;
        }

        public AuditLogBuilder outputData(String outputData) {
            this.outputData = outputData;
            return this;
        }

        public AuditLogBuilder status(ActionStatus status) {
            this.status = status;
            return this;
        }

        public AuditLogBuilder boundaryCheckPassed(Boolean boundaryCheckPassed) {
            this.boundaryCheckPassed = boundaryCheckPassed;
            return this;
        }

        public AuditLogBuilder boundaryDetails(String boundaryDetails) {
            this.boundaryDetails = boundaryDetails;
            return this;
        }

        public AuditLog build() {
            return new AuditLog(this.id, this.sessionId, this.timestamp, this.actionType, this.description, this.agentReasoning, this.inputData, this.outputData, this.status, this.boundaryCheckPassed, this.boundaryDetails);
        }
    }
}
