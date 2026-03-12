package com.crm.integration.repository;

import com.crm.integration.entity.WebhookDeliveryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookDeliveryLogRepository extends JpaRepository<WebhookDeliveryLog, UUID> {
    List<WebhookDeliveryLog> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<WebhookDeliveryLog> findByWebhook_IdAndTenantIdOrderByCreatedAtDesc(UUID webhookId, String tenantId);
    List<WebhookDeliveryLog> findByStatusAndTenantId(String status, String tenantId);
}
