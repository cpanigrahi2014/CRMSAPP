package com.crm.integration.repository;

import com.crm.integration.entity.CalendarSyncConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CalendarSyncConfigRepository extends JpaRepository<CalendarSyncConfig, UUID> {

    List<CalendarSyncConfig> findByTenantIdAndUserId(String tenantId, String userId);

    Optional<CalendarSyncConfig> findByTenantIdAndUserIdAndProvider(String tenantId, String userId, String provider);

    List<CalendarSyncConfig> findByEnabledTrueAndStatus(String status);

    Optional<CalendarSyncConfig> findByIdAndTenantId(UUID id, String tenantId);
}
