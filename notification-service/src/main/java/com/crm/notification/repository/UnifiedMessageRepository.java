package com.crm.notification.repository;

import com.crm.notification.entity.UnifiedMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UnifiedMessageRepository extends JpaRepository<UnifiedMessage, UUID> {
    Page<UnifiedMessage> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);
    Page<UnifiedMessage> findByTenantIdAndChannelOrderByCreatedAtDesc(String tenantId, String channel, Pageable pageable);
    Page<UnifiedMessage> findByTenantIdAndRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc(
            String tenantId, String relatedEntityType, String relatedEntityId, Pageable pageable);
}
