package com.crm.integration.repository;

import com.crm.integration.entity.DataSync;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DataSyncRepository extends JpaRepository<DataSync, UUID> {
    List<DataSync> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<DataSync> findByIdAndTenantId(UUID id, String tenantId);
}
