package com.crm.ai.repository;

import com.crm.ai.entity.AutoLeadRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AutoLeadRepository extends JpaRepository<AutoLeadRecord, UUID> {
    List<AutoLeadRecord> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<AutoLeadRecord> findByStatusAndTenantId(String status, String tenantId);
    List<AutoLeadRecord> findBySourceTypeAndTenantId(String sourceType, String tenantId);
    Optional<AutoLeadRecord> findByIdAndTenantId(UUID id, String tenantId);
}
