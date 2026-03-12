package com.crm.email.repository;

import com.crm.email.entity.EmailMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailMessageRepository extends JpaRepository<EmailMessage, UUID> {

    Page<EmailMessage> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
    Optional<EmailMessage> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<EmailMessage> findByTenantIdAndDirectionAndDeletedFalse(
            String tenantId, EmailMessage.Direction direction, Pageable pageable);

    Page<EmailMessage> findByTenantIdAndStatusAndDeletedFalse(
            String tenantId, EmailMessage.Status status, Pageable pageable);

    // Conversation thread
    List<EmailMessage> findByTenantIdAndThreadIdAndDeletedFalseOrderByCreatedAtAsc(
            String tenantId, String threadId);

    // Related entity messages (for conversation logging)
    Page<EmailMessage> findByTenantIdAndRelatedEntityTypeAndRelatedEntityIdAndDeletedFalse(
            String tenantId, String entityType, UUID entityId, Pageable pageable);

    // Search
    @Query("SELECT m FROM EmailMessage m WHERE m.tenantId = :tenantId AND m.deleted = false " +
           "AND (LOWER(m.subject) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(m.toAddresses) LIKE LOWER(CONCAT('%',:q,'%')) " +
           "OR LOWER(m.fromAddress) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<EmailMessage> search(@Param("tenantId") String tenantId, @Param("q") String q, Pageable pageable);

    // Scheduled emails ready to send
    @Query("SELECT m FROM EmailMessage m WHERE m.deleted = false AND m.status = 'QUEUED' " +
           "AND m.scheduledAt IS NOT NULL AND m.scheduledAt <= :now")
    List<EmailMessage> findScheduledReadyToSend(@Param("now") LocalDateTime now);

    // Analytics queries
    long countByTenantIdAndDeletedFalse(String tenantId);
    long countByTenantIdAndStatusAndDeletedFalse(String tenantId, EmailMessage.Status status);
    long countByTenantIdAndDirectionAndDeletedFalse(String tenantId, EmailMessage.Direction direction);
    long countByTenantIdAndOpenedTrueAndDeletedFalse(String tenantId);

    @Query("SELECT COUNT(m) FROM EmailMessage m WHERE m.tenantId = :tenantId AND m.deleted = false AND m.clickCount > 0")
    long countWithClicks(@Param("tenantId") String tenantId);

    @Query(value = "SELECT TO_CHAR(sent_at, 'YYYY-MM-DD') as day, COUNT(*) " +
           "FROM email_messages WHERE tenant_id = :tenantId AND deleted = false AND sent_at IS NOT NULL " +
           "AND sent_at >= :since GROUP BY day ORDER BY day", nativeQuery = true)
    List<Object[]> countSentByDay(@Param("tenantId") String tenantId, @Param("since") LocalDateTime since);

    @Modifying
    @Query("UPDATE EmailMessage m SET m.openCount = m.openCount + 1, m.opened = true, " +
           "m.firstOpenedAt = CASE WHEN m.firstOpenedAt IS NULL THEN :now ELSE m.firstOpenedAt END " +
           "WHERE m.id = :id")
    void recordOpen(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE EmailMessage m SET m.clickCount = m.clickCount + 1 WHERE m.id = :id")
    void recordClick(@Param("id") UUID id);
}
