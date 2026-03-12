package com.crm.auth.repository;

import com.crm.auth.entity.TenantPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantPlanRepository extends JpaRepository<TenantPlan, UUID> {
    Optional<TenantPlan> findByTenantId(String tenantId);
    boolean existsByTenantId(String tenantId);
}
