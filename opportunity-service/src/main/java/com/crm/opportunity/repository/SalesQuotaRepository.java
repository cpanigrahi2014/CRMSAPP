package com.crm.opportunity.repository;

import com.crm.opportunity.entity.SalesQuota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SalesQuotaRepository extends JpaRepository<SalesQuota, UUID> {

    List<SalesQuota> findByTenantIdAndDeletedFalse(String tenantId);

    List<SalesQuota> findByTenantIdAndUserIdAndDeletedFalse(String tenantId, String userId);

    Optional<SalesQuota> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    @Query("SELECT q FROM SalesQuota q WHERE q.tenantId = :tenantId AND q.deleted = false " +
           "AND q.periodStart <= :date AND q.periodEnd >= :date")
    List<SalesQuota> findActiveQuotas(String tenantId, LocalDate date);

    @Query("SELECT q FROM SalesQuota q WHERE q.tenantId = :tenantId AND q.userId = :userId " +
           "AND q.deleted = false AND q.periodStart <= :date AND q.periodEnd >= :date")
    Optional<SalesQuota> findCurrentQuota(String tenantId, String userId, LocalDate date);

    @Query("SELECT q FROM SalesQuota q WHERE q.tenantId = :tenantId AND q.deleted = false " +
           "AND q.periodType = :periodType AND q.periodStart >= :start AND q.periodEnd <= :end")
    List<SalesQuota> findByPeriod(String tenantId, SalesQuota.PeriodType periodType,
                                   LocalDate start, LocalDate end);
}
