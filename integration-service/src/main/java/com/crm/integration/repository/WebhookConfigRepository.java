package com.crm.integration.repository;

import com.crm.integration.entity.WebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WebhookConfigRepository extends JpaRepository<WebhookConfig, UUID> {
    List<WebhookConfig> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<WebhookConfig> findByIdAndTenantId(UUID id, String tenantId);
}
