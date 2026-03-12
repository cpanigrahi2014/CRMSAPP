package com.crm.opportunity.repository;

import com.crm.opportunity.entity.StageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface StageHistoryRepository extends JpaRepository<StageHistory, UUID> {

    List<StageHistory> findByOpportunityIdOrderByChangedAtDesc(UUID opportunityId);

    List<StageHistory> findByTenantIdOrderByChangedAtDesc(String tenantId);

    List<StageHistory> findByTenantIdAndChangedAtBetweenOrderByChangedAtDesc(
            String tenantId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT sh.fromStage, sh.toStage, COUNT(sh) FROM StageHistory sh " +
           "WHERE sh.tenantId = :tenantId AND sh.fromStage IS NOT NULL " +
           "GROUP BY sh.fromStage, sh.toStage")
    List<Object[]> getStageTransitionCounts(String tenantId);

    @Query("SELECT sh.toStage, AVG(sh.timeInStage) FROM StageHistory sh " +
           "WHERE sh.tenantId = :tenantId AND sh.timeInStage IS NOT NULL " +
           "GROUP BY sh.toStage")
    List<Object[]> getAvgTimeInStage(String tenantId);

    @Query("SELECT sh.fromStage, sh.toStage, COUNT(sh) FROM StageHistory sh " +
           "WHERE sh.tenantId = :tenantId AND sh.fromStage IS NOT NULL " +
           "AND sh.changedAt BETWEEN :start AND :end " +
           "GROUP BY sh.fromStage, sh.toStage")
    List<Object[]> getStageTransitionCountsBetween(String tenantId, LocalDateTime start, LocalDateTime end);

    long countByTenantIdAndToStage(String tenantId, String toStage);
}
