package com.crm.integration.repository;

import com.crm.integration.entity.IntegrationErrorRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntegrationErrorRepository extends JpaRepository<IntegrationErrorRecord, UUID> {
    List<IntegrationErrorRecord> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<IntegrationErrorRecord> findByTenantIdAndLevelOrderByCreatedAtDesc(String tenantId, String level);
}
