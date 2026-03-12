package com.crm.integration.repository;

import com.crm.integration.entity.ApiAuthConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiAuthConfigRepository extends JpaRepository<ApiAuthConfig, UUID> {
    List<ApiAuthConfig> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<ApiAuthConfig> findByIdAndTenantId(UUID id, String tenantId);
}
