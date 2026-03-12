package com.crm.notification.repository;

import com.crm.notification.entity.WhatsAppMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WhatsAppMessageRepository extends JpaRepository<WhatsAppMessage, UUID> {
    Page<WhatsAppMessage> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);
    Page<WhatsAppMessage> findByTenantIdAndDirectionAndDeletedFalse(String tenantId, WhatsAppMessage.Direction direction, Pageable pageable);
    Page<WhatsAppMessage> findByTenantIdAndToNumberAndDeletedFalse(String tenantId, String toNumber, Pageable pageable);
    Optional<WhatsAppMessage> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);
}
