package com.crm.ai.repository;

import com.crm.ai.entity.SalesForecastRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesForecastRepository extends JpaRepository<SalesForecastRecord, UUID> {
    List<SalesForecastRecord> findByTenantIdOrderByPeriodAsc(String tenantId);
    Optional<SalesForecastRecord> findByPeriodAndTenantId(String period, String tenantId);
    Optional<SalesForecastRecord> findByIdAndTenantId(UUID id, String tenantId);
}
