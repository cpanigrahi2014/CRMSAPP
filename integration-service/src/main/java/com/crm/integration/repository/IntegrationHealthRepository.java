package com.crm.integration.repository;

import com.crm.integration.entity.IntegrationHealthRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IntegrationHealthRepository extends JpaRepository<IntegrationHealthRecord, UUID> {
    List<IntegrationHealthRecord> findByTenantIdOrderByLastCheckedAtDesc(String tenantId);
}
