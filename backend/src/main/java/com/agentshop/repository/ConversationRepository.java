package com.agentshop.repository;

import com.agentshop.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findBySessionIdOrderByTimestampAsc(String sessionId);

    @Query("SELECT DISTINCT c.sessionId FROM Conversation c ORDER BY c.sessionId")
    List<String> findAllSessionIds();

    long countBySessionId(String sessionId);
}
