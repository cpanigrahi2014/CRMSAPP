package com.crm.opportunity.repository;

import com.crm.opportunity.entity.Opportunity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OpportunityRepository extends JpaRepository<Opportunity, UUID> {

    Page<Opportunity> findByTenantIdAndDeletedFalse(String tenantId, Pageable pageable);

    Optional<Opportunity> findByIdAndTenantIdAndDeletedFalse(UUID id, String tenantId);

    Page<Opportunity> findByTenantIdAndStageAndDeletedFalse(String tenantId, Opportunity.OpportunityStage stage, Pageable pageable);

    Page<Opportunity> findByTenantIdAndAccountIdAndDeletedFalse(String tenantId, UUID accountId, Pageable pageable);

    Page<Opportunity> findByTenantIdAndAssignedToAndDeletedFalse(String tenantId, UUID assignedTo, Pageable pageable);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId AND o.deleted = false " +
            "AND (LOWER(o.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(o.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Opportunity> searchOpportunities(@Param("tenantId") String tenantId, @Param("search") String search, Pageable pageable);

    long countByTenantIdAndStageAndDeletedFalse(String tenantId, Opportunity.OpportunityStage stage);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Opportunity o WHERE o.tenantId = :tenantId " +
            "AND o.stage = :stage AND o.deleted = false")
    java.math.BigDecimal sumAmountByTenantIdAndStage(@Param("tenantId") String tenantId, @Param("stage") Opportunity.OpportunityStage stage);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Opportunity o WHERE o.tenantId = :tenantId " +
            "AND o.forecastCategory = :category AND o.deleted = false")
    java.math.BigDecimal sumAmountByTenantIdAndForecastCategory(@Param("tenantId") String tenantId, @Param("category") Opportunity.ForecastCategory category);

    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId AND o.deleted = false " +
            "AND o.closeDate BETWEEN :startDate AND :endDate")
    List<Opportunity> findByTenantIdAndCloseDateBetweenAndDeletedFalse(
            @Param("tenantId") String tenantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT o.stage, COUNT(o), COALESCE(SUM(o.amount), 0) FROM Opportunity o " +
            "WHERE o.tenantId = :tenantId AND o.deleted = false GROUP BY o.stage")
    List<Object[]> getRevenueByStage(@Param("tenantId") String tenantId);

    long countByTenantIdAndStageInAndDeletedFalse(String tenantId, List<Opportunity.OpportunityStage> stages);

    long countByTenantIdAndDeletedFalse(String tenantId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Opportunity o WHERE o.tenantId = :tenantId AND o.deleted = false")
    java.math.BigDecimal sumAllAmountByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Opportunity o WHERE o.tenantId = :tenantId AND o.stage = 'CLOSED_WON' AND o.deleted = false")
    java.math.BigDecimal sumWonAmount(@Param("tenantId") String tenantId);

    @Query("SELECT COALESCE(SUM(o.amount), 0) FROM Opportunity o WHERE o.tenantId = :tenantId AND o.stage = 'CLOSED_LOST' AND o.deleted = false")
    java.math.BigDecimal sumLostAmount(@Param("tenantId") String tenantId);

    List<Opportunity> findByTenantIdAndStageAndDeletedFalse(String tenantId, Opportunity.OpportunityStage stage);

    @Query("SELECT o.leadSource, COALESCE(SUM(o.amount), 0) FROM Opportunity o WHERE o.tenantId = :tenantId AND o.deleted = false AND o.leadSource IS NOT NULL GROUP BY o.leadSource")
    List<Object[]> getRevenueByLeadSource(@Param("tenantId") String tenantId);

    @Query("SELECT o.lostReason, COUNT(o) FROM Opportunity o WHERE o.tenantId = :tenantId AND o.stage = 'CLOSED_LOST' AND o.deleted = false AND o.lostReason IS NOT NULL GROUP BY o.lostReason")
    List<Object[]> getLostReasonBreakdown(@Param("tenantId") String tenantId);

    // ── Per-assignee analytics ──────────────────────────────
    @Query("SELECT o.assignedTo, o.stage, COUNT(o), COALESCE(SUM(o.amount), 0) " +
           "FROM Opportunity o WHERE o.tenantId = :tenantId AND o.deleted = false " +
           "AND o.assignedTo IS NOT NULL GROUP BY o.assignedTo, o.stage")
    List<Object[]> getPerAssigneeStageBreakdown(@Param("tenantId") String tenantId);

    // All opportunities (non-paged, for analytics)
    List<Opportunity> findByTenantIdAndDeletedFalse(String tenantId);

    // Won opportunities with wonDate for cycle-time calculations
    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId AND o.stage = 'CLOSED_WON' " +
           "AND o.deleted = false AND o.wonDate IS NOT NULL")
    List<Opportunity> findWonOpportunitiesWithDate(@Param("tenantId") String tenantId);

    // Count by stage entry via stage history (won deals that entered from each stage)
    @Query("SELECT o.stage, COUNT(o) FROM Opportunity o WHERE o.tenantId = :tenantId " +
           "AND o.stage = 'CLOSED_WON' AND o.deleted = false GROUP BY o.stage")
    List<Object[]> countWonByEntryStage(@Param("tenantId") String tenantId);

    // Pipeline view - all non-deleted opportunities ordered by stage
    @Query("SELECT o FROM Opportunity o WHERE o.tenantId = :tenantId AND o.deleted = false ORDER BY o.stage, o.amount DESC")
    List<Opportunity> findAllForPipeline(@Param("tenantId") String tenantId);

    // Monthly revenue trend — won deals grouped by year-month of wonDate
    @Query("SELECT FUNCTION('TO_CHAR', o.wonDate, 'YYYY-MM'), COUNT(o), COALESCE(SUM(o.amount), 0) " +
           "FROM Opportunity o WHERE o.tenantId = :tenantId AND o.stage = 'CLOSED_WON' " +
           "AND o.deleted = false AND o.wonDate IS NOT NULL " +
           "GROUP BY FUNCTION('TO_CHAR', o.wonDate, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', o.wonDate, 'YYYY-MM')")
    List<Object[]> getMonthlyWonRevenue(@Param("tenantId") String tenantId);

    // Monthly deal creation trend — grouped by year-month of createdAt
    @Query("SELECT FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM'), COUNT(o), COALESCE(SUM(o.amount), 0) " +
           "FROM Opportunity o WHERE o.tenantId = :tenantId AND o.deleted = false " +
           "GROUP BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM') " +
           "ORDER BY FUNCTION('TO_CHAR', o.createdAt, 'YYYY-MM')")
    List<Object[]> getMonthlyCreatedDeals(@Param("tenantId") String tenantId);
}
