package com.crm.email.repository;

import com.crm.email.entity.EmailTrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailTrackingEventRepository extends JpaRepository<EmailTrackingEvent, UUID> {

    List<EmailTrackingEvent> findByMessageIdOrderByCreatedAtDesc(UUID messageId);
    long countByMessageIdAndEventType(UUID messageId, EmailTrackingEvent.EventType eventType);

    @Query("SELECT e.eventType, COUNT(e) FROM EmailTrackingEvent e " +
           "WHERE e.messageId IN (SELECT m.id FROM EmailMessage m WHERE m.tenantId = :tenantId AND m.deleted = false) " +
           "GROUP BY e.eventType")
    List<Object[]> countByEventType(@Param("tenantId") String tenantId);

    @Query(value = "SELECT TO_CHAR(e.created_at, 'YYYY-MM-DD') as day, COUNT(*) " +
           "FROM email_tracking_events e JOIN email_messages m ON e.message_id = m.id " +
           "WHERE m.tenant_id = :tenantId AND m.deleted = false AND e.event_type = :eventType " +
           "AND e.created_at >= :since GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> countEventsByDay(@Param("tenantId") String tenantId,
                                    @Param("eventType") String eventType,
                                    @Param("since") LocalDateTime since);
}
