package com.crm.integration.repository;

import com.crm.integration.entity.ApiEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiEndpointRepository extends JpaRepository<ApiEndpoint, UUID> {
    List<ApiEndpoint> findByTenantIdOrderByNameAsc(String tenantId);
    Optional<ApiEndpoint> findByIdAndTenantId(UUID id, String tenantId);
}
