package com.agentshop.repository;

import com.agentshop.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findBySessionIdOrderByTimestampDesc(String sessionId);

    List<AuditLog> findByActionTypeOrderByTimestampDesc(AuditLog.ActionType actionType);

    List<AuditLog> findAllByOrderByTimestampDesc();

    List<AuditLog> findByTimestampAfterOrderByTimestampDesc(java.time.LocalDateTime date);
}
