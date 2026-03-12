package com.crm.integration.repository;

import com.crm.integration.entity.CustomApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomAppRepository extends JpaRepository<CustomApp, UUID> {
    List<CustomApp> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<CustomApp> findByIdAndTenantId(UUID id, String tenantId);
    Optional<CustomApp> findBySlugAndTenantId(String slug, String tenantId);
}
