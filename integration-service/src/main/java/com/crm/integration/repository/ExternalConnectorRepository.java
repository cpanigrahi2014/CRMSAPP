package com.crm.integration.repository;

import com.crm.integration.entity.ExternalConnector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalConnectorRepository extends JpaRepository<ExternalConnector, UUID> {
    List<ExternalConnector> findByTenantIdOrderByNameAsc(String tenantId);
    Optional<ExternalConnector> findByIdAndTenantId(UUID id, String tenantId);
}
