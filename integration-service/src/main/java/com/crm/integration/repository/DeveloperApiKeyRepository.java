package com.crm.integration.repository;

import com.crm.integration.entity.DeveloperApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeveloperApiKeyRepository extends JpaRepository<DeveloperApiKey, UUID> {
    List<DeveloperApiKey> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<DeveloperApiKey> findByIdAndTenantId(UUID id, String tenantId);
    Optional<DeveloperApiKey> findByKeyPrefixAndTenantId(String keyPrefix, String tenantId);
}
