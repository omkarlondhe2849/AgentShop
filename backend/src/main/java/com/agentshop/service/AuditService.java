package com.agentshop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.agentshop.model.AuditLog;
import com.agentshop.repository.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.scheduling.annotation.Async;

@Service
public class AuditService {

    public AuditService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);


    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void logAction(String sessionId,
                              AuditLog.ActionType actionType,
                              String description,
                              String agentReasoning,
                              Object inputData,
                              Object outputData,
                              AuditLog.ActionStatus status,
                              Boolean boundaryCheckPassed,
                              String boundaryDetails) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .sessionId(sessionId)
                    .actionType(actionType)
                    .description(description)
                    .agentReasoning(agentReasoning)
                    .inputData(inputData != null ? objectMapper.writeValueAsString(inputData) : null)
                    .outputData(outputData != null ? objectMapper.writeValueAsString(outputData) : null)
                    .status(status)
                    .boundaryCheckPassed(boundaryCheckPassed)
                    .boundaryDetails(boundaryDetails)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("AUDIT [{}] {} — {} — {}", sessionId, actionType, status, description);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }

    public List<AuditLog> getAuditTrail(String sessionId, String range) {
        if (sessionId != null && !sessionId.isEmpty()) {
            return auditLogRepository.findBySessionIdOrderByTimestampDesc(sessionId);
        }

        if ("7days".equals(range)) {
            return auditLogRepository.findByTimestampAfterOrderByTimestampDesc(java.time.LocalDateTime.now().minusDays(7));
        } else if ("30days".equals(range)) {
            return auditLogRepository.findByTimestampAfterOrderByTimestampDesc(java.time.LocalDateTime.now().minusDays(30));
        }

        return auditLogRepository.findAllByOrderByTimestampDesc();
    }

    public List<AuditLog> getByActionType(AuditLog.ActionType actionType) {
        return auditLogRepository.findByActionTypeOrderByTimestampDesc(actionType);
    }
}
