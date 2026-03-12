package com.crm.integration.repository;

import com.crm.integration.entity.ThirdPartyIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ThirdPartyIntegrationRepository extends JpaRepository<ThirdPartyIntegration, UUID> {
    List<ThirdPartyIntegration> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<ThirdPartyIntegration> findByTenantIdAndEnabledTrue(String tenantId);
    Optional<ThirdPartyIntegration> findByIdAndTenantId(UUID id, String tenantId);
}
