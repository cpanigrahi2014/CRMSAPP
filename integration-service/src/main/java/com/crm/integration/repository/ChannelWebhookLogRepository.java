package com.crm.integration.repository;

import com.crm.integration.entity.ChannelWebhookLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChannelWebhookLogRepository extends JpaRepository<ChannelWebhookLog, UUID> {

    Page<ChannelWebhookLog> findByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    List<ChannelWebhookLog> findByTenantIdAndChannelOrderByCreatedAtDesc(String tenantId, String channel);

    List<ChannelWebhookLog> findByTenantIdAndLeadIdOrderByCreatedAtDesc(String tenantId, UUID leadId);

    List<ChannelWebhookLog> findByTenantIdAndCaseIdOrderByCreatedAtDesc(String tenantId, UUID caseId);
}
