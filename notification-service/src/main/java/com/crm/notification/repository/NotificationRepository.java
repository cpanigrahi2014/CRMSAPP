package com.crm.notification.repository;

import com.crm.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Optional<Notification> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<Notification> findByTenantIdAndStatusAndDeletedFalse(
            String tenantId, Notification.NotificationStatus status, Pageable pageable);

    Page<Notification> findByTenantIdAndRecipientAndDeletedFalse(
            String tenantId, String recipient, Pageable pageable);

    Page<Notification> findByTenantIdAndTypeAndDeletedFalse(
            String tenantId, Notification.NotificationType type, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.deleted = false " +
            "AND n.status = :status AND n.type = :type")
    Page<Notification> findByTenantIdAndStatusAndType(
            @Param("tenantId") String tenantId,
            @Param("status") Notification.NotificationStatus status,
            @Param("type") Notification.NotificationType type,
            Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.tenantId = :tenantId AND n.deleted = false " +
            "AND n.status = 'PENDING' ORDER BY n.createdAt ASC")
    Page<Notification> findPendingNotifications(
            @Param("tenantId") String tenantId, Pageable pageable);

    long countByTenantIdAndStatusAndDeletedFalse(String tenantId, Notification.NotificationStatus status);
}
