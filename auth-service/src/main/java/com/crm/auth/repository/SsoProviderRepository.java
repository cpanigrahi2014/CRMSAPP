package com.crm.auth.repository;

import com.crm.auth.entity.SsoProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SsoProviderRepository extends JpaRepository<SsoProvider, UUID> {

    List<SsoProvider> findByTenantIdOrderByNameAsc(String tenantId);

    List<SsoProvider> findByEnabledTrueAndTenantId(String tenantId);

    Optional<SsoProvider> findByIdAndTenantId(UUID id, String tenantId);

    Optional<SsoProvider> findByNameAndTenantId(String name, String tenantId);

    boolean existsByNameAndTenantId(String name, String tenantId);
}
