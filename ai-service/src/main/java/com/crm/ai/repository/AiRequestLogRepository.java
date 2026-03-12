package com.crm.ai.repository;

import com.crm.ai.entity.AiRequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiRequestLogRepository extends JpaRepository<AiRequestLog, UUID> {

    List<AiRequestLog> findByTenantIdOrderByCreatedAtDesc(String tenantId);

    List<AiRequestLog> findByTenantIdAndRequestTypeOrderByCreatedAtDesc(String tenantId, String requestType);
}
