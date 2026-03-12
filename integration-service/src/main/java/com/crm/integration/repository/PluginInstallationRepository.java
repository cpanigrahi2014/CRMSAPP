package com.crm.integration.repository;

import com.crm.integration.entity.PluginInstallation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PluginInstallationRepository extends JpaRepository<PluginInstallation, UUID> {
    List<PluginInstallation> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    Optional<PluginInstallation> findByPlugin_IdAndTenantId(UUID pluginId, String tenantId);
    List<PluginInstallation> findByTenantIdAndStatus(String tenantId, String status);
}
